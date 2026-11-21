package spiralcraft.net.http.client.sse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups the lines of an event stream into events (WHATWG HTML,
 *   "Server-sent events", interpreting an event stream).
 *
 * Each call to next() returns an EVENT, a COMMENT for a comment line,
 *   or END, which repeats. The last id seen, its sequence, and the last
 *   valid retry value are carried on every event. A partial event at
 *   the end of the stream is not delivered; the END item carries its
 *   lines, and a partial last line, when lines are kept.
 */
public class SseReader
  implements AutoCloseable
{
  private final SseLineReader lines;
  private final boolean keepLines;
  private final List<SseLine> pending=new ArrayList<>();
  private final StringBuilder data=new StringBuilder();
  private boolean hasData;
  private String eventName;
  private String lastId;
  private int nextSequence;
  private int retryMs=-1;
  private SseEvent end;

  public SseReader(SseLineReader lines,boolean keepLines)
  {
    this.lines=lines;
    this.keepLines=keepLines;
  }

  public SseReader(InputStream in,boolean keepLines)
  { this(new SseLineReader(in),keepLines);
  }

  /**
   * The next item. IOException comes from the line reader, which keeps
   *   failing after the first failure.
   */
  public SseEvent next()
    throws IOException
  {
    if (end!=null)
    { return end;
    }
    while (true)
    {
      SseLine line=lines.next();
      switch (line.kind())
      {
        case FIELD:
          field(line);
          break;
        case COMMENT:
          return comment(line);
        case BLANK:
        {
          SseEvent event=dispatch();
          if (event!=null)
          { return event;
          }
          break;
        }
        case PARTIAL:
          keep(line);
          break;
        case END:
          end=ending();
          return end;
      }
    }
  }

  @Override
  public void close()
  { lines.close();
  }

  private void keep(SseLine line)
  {
    if (keepLines)
    { pending.add(line);
    }
  }

  private void field(SseLine line)
  {
    keep(line);
    String value=line.value();
    switch (line.field())
    {
      case "event":
        eventName=value;
        break;
      case "data":
        data.append(value).append('\n');
        hasData=true;
        break;
      case "id":
        if (value.indexOf('\0')<0)
        {
          lastId=value;
          nextSequence=0;
        }
        break;
      case "retry":
        parseRetry(value);
        break;
      default:
        break;
    }
  }

  /** ASCII digits only; a larger value than an int holds is clamped */
  private void parseRetry(String value)
  {
    if (value.isEmpty())
    { return;
    }
    long ms=0;
    for (int i=0;i<value.length();i++)
    {
      char c=value.charAt(i);
      if (c<'0' || c>'9')
      { return;
      }
      ms=Math.min(ms*10+(c-'0'),Integer.MAX_VALUE);
    }
    retryMs=(int) ms;
  }

  private SseEvent comment(SseLine line)
  {
    return new SseEvent
      (SseEvent.Kind.COMMENT,null,null,null,0,-1,line.value()
      ,keepLines?List.of(line):List.of()
      );
  }

  /** An event, or null if there was no data to dispatch */
  private SseEvent dispatch()
  {
    if (!hasData)
    {
      reset();
      return null;
    }
    String text=data.substring(0,data.length()-1);
    String name=(eventName==null || eventName.isEmpty())?null:eventName;
    int sequence=(lastId==null)?0:nextSequence++;
    List<SseLine> kept=keepLines?List.copyOf(pending):List.of();
    reset();
    return new SseEvent
      (SseEvent.Kind.EVENT,name,text,lastId,sequence,retryMs,null,kept);
  }

  private SseEvent ending()
  {
    List<SseLine> kept=keepLines?List.copyOf(pending):List.of();
    reset();
    return new SseEvent
      (SseEvent.Kind.END,null,null,null,0,-1,null,kept);
  }

  private void reset()
  {
    data.setLength(0);
    hasData=false;
    eventName=null;
    pending.clear();
  }
}
