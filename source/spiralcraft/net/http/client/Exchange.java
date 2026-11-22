package spiralcraft.net.http.client;

import java.io.IOException;
import java.io.OutputStream;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;

/**
 * One request and its response, created by a Client with Client.open().
 *   Opening does no network I/O; start() connects, sends the request and
 *   reads the response head, after which the body streams through
 *   Response.getInputStream(), and complete() buffers the body into
 *   Response.getContent().
 *
 * One thread drives start(), complete() and the reads. close() may be
 *   called from any thread, in any phase, more than once: it cancels the
 *   exchange, and the IOException that a blocked call then throws is the
 *   cancel, told from a failure by isClosed().
 */
public class Exchange
  implements AutoCloseable
{
  private static final ClassLog log
    =ClassLog.getInstance(Exchange.class);

  private final Client client;
  private final HttpConnection connection;
  private final Request request;
  private final Level logLevel;
  private volatile Response response;
  private volatile boolean closed;
  private boolean started;
  private boolean completed;

  Exchange
    (Client client,HttpConnection connection,Request request,Level logLevel)
  {
    this.client=client;
    this.connection=connection;
    this.request=request;
    this.logLevel=logLevel;
  }

  /** The Client that opened this exchange */
  public Client getClient()
  { return client;
  }

  public Request getRequest()
  { return request;
  }

  /** The response, once start() has read its head; otherwise null */
  public Response getResponse()
  { return response;
  }

  /**
   * Connect, send the request and read the response head. The body then
   *   streams through Response.getInputStream(). Runs once.
   */
  public Response start()
    throws IOException
  {
    if (started)
    { throw new IllegalStateException("Exchange already started");
    }
    started=true;
    checkClosed();
    try
    {
      connection.startRequest(request);
      connection.completeRequest();

      Response response=connection.startStreamingResponse();
      response.setExchange(this);
      this.response=response;
      client.takeCookies(response);

      if (logLevel.isFine())
      { log.fine("Got response: request: "+request+" response:"+response);
      }
      return response;
    }
    catch (IOException | RuntimeException x)
    {
      close();
      throw x;
    }
  }

  /**
   * Start the exchange if not yet started, then read the whole body into
   *   Response.getContent(), which stays usable after the exchange closes.
   *   The exchange is closed when this returns. Refused after the body was
   *   read as a stream.
   */
  public Response complete()
    throws IOException
  {
    if (completed)
    { throw new IllegalStateException("Exchange already completed");
    }
    if (!started)
    { start();
    }
    else if (response==null)
    { throw new IllegalStateException("Exchange did not start");
    }
    else if (response.getInputStream().wasRead())
    { throw new IllegalStateException("Body was read as a stream");
    }
    completed=true;

    try
    {
      ByteArrayResource content=new ByteArrayResource();
      try (OutputStream out=content.getOutputStream())
      {
        StreamUtil.copyRaw(response.getInputStream(),out,2048,-1);
        out.flush(); // ByteArrayResource takes the bytes on flush
      }
      response.setContent(content);
      return response;
    }
    finally
    { close();
    }
  }

  /**
   * Cancel or finish the exchange: closes the response stream and the
   *   connection. Safe from any thread, in any phase, more than once.
   */
  @Override
  public void close()
  {
    closed=true;
    Response response=this.response;
    if (response!=null)
    { response.closeStream();
    }
    connection.close();
  }

  /** Whether close() was called: an IOException after it is the cancel */
  public boolean isClosed()
  { return closed;
  }

  private void checkClosed()
    throws IOException
  {
    if (closed)
    { throw new IOException("Exchange closed");
    }
  }
}
