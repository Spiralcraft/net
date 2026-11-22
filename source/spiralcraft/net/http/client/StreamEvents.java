package spiralcraft.net.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.net.http.client.sse.SseEvent;
import spiralcraft.net.http.client.sse.SseReader;

/**
 * Requests a URI that streams server-sent events, publishes each item (event,
 *   comment and end) into the focus chain, found by type, and runs the
 *   chain once for each. A response that is not a successful event stream
 *   fails the task. stop() closes the connection at once.
 */
public class StreamEvents
  extends ClientPlan<Void,Void>
{
  /** The most characters of a failed response body put in the failure */
  private static final int SNIPPET_CHARS=1024;

  private boolean logEvents=false;

  private boolean keepLines;
  private ThreadLocalChannel<SseEvent> eventChannel;

  /** Whether each item holds the lines it was made from. Default false. */
  public void setKeepLines(boolean keepLines)
  { this.keepLines=keepLines;
  }

  public void setLogEvents(boolean val)
  { this.logEvents=val;
  }

  @Override
  public Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    eventChannel
      =new ThreadLocalChannel<SseEvent>
        (BeanReflector.<SseEvent>getInstance(SseEvent.class));
    return focus.chain(eventChannel);
  }

  @Override
  protected StreamEventsTask task()
  { return new StreamEventsTask();
  }

  protected class StreamEventsTask
    extends ExchangeTask
  {
    private URI uri;

    @Override
    protected void work()
      throws InterruptedException
    {
      Exchange exchange=null;
      try
      {
        uri=uri();
        exchange=openExchange(uri);
        Response r=exchange.start();
        if (isStopRequested())
        { return;
        }
        if (!isEventStream(r))
        { 
          addException
            (new ContextualException
              ("Not an event stream: "+r.getStatus()+" "+r.getReason()
               +" "+contentTypeOf(r)+" "+snippet(r)
              ,getDeclarationInfo()
              )
            );
          return;
        }
        run(r);
      }
      catch (IOException x)
      { 
        if (!isCancelled())
        { 
          addException
            (new ContextualException
              ("Error streaming "+uri,getDeclarationInfo(),x)
            );
        }
      }
      finally
      { 
        if (exchange!=null)
        { exchange.close();
        }
      }
    }

    private void run(Response r)
      throws IOException,InterruptedException
    {
      try (SseReader reader=new SseReader(r.getInputStream(),keepLines))
      {
        while (!isStopRequested() && exception==null)
        {
          SseEvent event=reader.next();
          eventChannel.push(event);
          if (logEvents)
          { log.info("Event from "+uri+": "+event);
          }
          try
          { super.work();
          }
          finally
          { eventChannel.pop();
          }
          if (event.kind()==SseEvent.Kind.END)
          { break;
          }
        }
      }
    }

    private boolean isEventStream(Response r)
    { 
      return r.getStatus()>=200 && r.getStatus()<300
        && contentTypeOf(r).toLowerCase().startsWith("text/event-stream");
    }

    private String contentTypeOf(Response r)
    { 
      String value=r.getHeader("Content-Type")!=null
        ?r.getHeader("Content-Type").getRawValue()
        :null;
      return value!=null?value.trim():"";
    }

    /** At most SNIPPET_CHARS characters from the start of the body */
    private String snippet(Response r)
    {
      try
      {
        InputStream in=r.getInputStream();
        byte[] bytes=new byte[SNIPPET_CHARS*4];
        int n=Math.max(in.read(bytes),0);
        String text=new String(bytes,0,n,StandardCharsets.UTF_8);
        return text.length()>SNIPPET_CHARS
          ?text.substring(0,SNIPPET_CHARS)
          :text;
      }
      catch (IOException x)
      { return "(body not readable: "+x+")";
      }
    }
  }
}