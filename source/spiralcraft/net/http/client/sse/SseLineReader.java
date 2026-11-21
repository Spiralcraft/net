package spiralcraft.net.http.client.sse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Reads an event stream as lines, each parsed into a field name and a
 *   value (WHATWG HTML, "Server-sent events", parsing an event stream).
 *   Keeps no state beyond buffering; SseReader groups lines into events.
 *
 * A line ends with CRLF, LF or CR. A leading byte order mark is
 *   dropped and each line is decoded as UTF-8. A line longer than the
 *   limit fails the stream. The text after the last line break, if
 *   any, comes back as a PARTIAL line, then END.
 */
public class SseLineReader
  implements AutoCloseable
{
  /** Longest line accepted by default, in bytes */
  public static final int DEFAULT_MAX_LINE_BYTES=16*1024*1024;

  private static final SseLine BLANK
    =new SseLine(SseLine.Kind.BLANK,null,null);
  private static final SseLine END
    =new SseLine(SseLine.Kind.END,null,null);

  private final InputStream in;
  private final int maxLineBytes;
  private final byte[] buf=new byte[8192];
  private int pos;
  private int limit;
  private byte[] line;
  private int lineLength;
  private boolean skipLF;
  private boolean firstLine=true;
  private boolean eof;
  private IOException failure;

  public SseLineReader(InputStream in)
  { this(in,DEFAULT_MAX_LINE_BYTES);
  }

  public SseLineReader(InputStream in,int maxLineBytes)
  {
    if (maxLineBytes<1)
    { throw new IllegalArgumentException("maxLineBytes<1: "+maxLineBytes);
    }
    this.in=in;
    this.maxLineBytes=maxLineBytes;
    this.line=new byte[Math.min(256,maxLineBytes)];
  }

  /**
   * The next line. After the stream ends this returns END every time.
   *   After a failure it throws, naming the first failure as the cause.
   */
  public SseLine next()
    throws IOException
  {
    if (failure!=null)
    { throw new IOException("Event stream failed earlier",failure);
    }
    try
    { return readLine();
    }
    catch (IOException x)
    {
      failure=x;
      throw x;
    }
  }

  /**
   * Closes the stream, which is also the cancel. A failure to close is
   *   thrown as an UncheckedIOException.
   */
  @Override
  public void close()
  {
    try
    { in.close();
    }
    catch (IOException x)
    { throw new UncheckedIOException(x);
    }
  }

  private SseLine readLine()
    throws IOException
  {
    if (eof)
    { return END;
    }
    while (true)
    {
      if (pos==limit)
      {
        pos=0;
        limit=in.read(buf,0,buf.length);
        if (limit<0)
        {
          limit=0;
          eof=true;
          return endOfStream();
        }
      }
      int c=buf[pos++] & 0xFF;
      if (skipLF)
      {
        skipLF=false;
        if (c=='\n')
        { continue;
        }
      }
      if (c=='\n' || c=='\r')
      {
        skipLF=(c=='\r');
        return parseLine();
      }
      append(c);
    }
  }

  /** The text after the last line break is partial: not parsed */
  private SseLine endOfStream()
  {
    String text=takeText();
    return text.isEmpty()
      ?END
      :new SseLine(SseLine.Kind.PARTIAL,null,text);
  }

  private void append(int c)
    throws IOException
  {
    if (lineLength==line.length)
    {
      if (lineLength>=maxLineBytes)
      {
        throw new IOException
          ("Line too long in event stream (limit "+maxLineBytes+" bytes)");
      }
      line=Arrays.copyOf(line,(int) Math.min(2L*line.length,maxLineBytes));
    }
    line[lineLength++]=(byte) c;
  }

  /** Decode and clear the line buffer, dropping a leading byte order mark */
  private String takeText()
  {
    int start=0;
    if (firstLine
        && lineLength>=3
        && line[0]==(byte) 0xEF
        && line[1]==(byte) 0xBB
        && line[2]==(byte) 0xBF
        )
    { start=3;
    }
    firstLine=false;
    String text
      =new String(line,start,lineLength-start,StandardCharsets.UTF_8);
    lineLength=0;
    return text;
  }

  private SseLine parseLine()
  {
    String text=takeText();
    if (text.isEmpty())
    { return BLANK;
    }
    int colon=text.indexOf(':');
    if (colon==0)
    { return new SseLine(SseLine.Kind.COMMENT,null,afterColon(text,1));
    }
    else if (colon<0)
    { return new SseLine(SseLine.Kind.FIELD,text,"");
    }
    else
    {
      return new SseLine
        (SseLine.Kind.FIELD,text.substring(0,colon),afterColon(text,colon+1));
    }
  }

  /** The text from the index on, less one leading space */
  private static String afterColon(String text,int index)
  {
    if (index<text.length() && text.charAt(index)==' ')
    { index++;
    }
    return text.substring(index);
  }
}
