package spiralcraft.net.http.client;

import java.io.IOException;
import java.net.URI;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;

/**
 * Requests a URI and publishes the Response into the focus chain, found by
 *   type, for the chain to run once. The response is buffered. A status
 *   outside 2xx is data in the Response, not an error.
 */
public class Fetch
  extends ClientPlan<Void,Void>
{
  private ThreadLocalChannel<Response> responseChannel;

  private boolean logResponse=false;

  public void setLogResponse(boolean val)
  { this.logResponse=val;
  }
  
  @Override
  public Focus<?> bindExports(Focus<?> focus)
    throws ContextualException
  {
    responseChannel
      =new ThreadLocalChannel<Response>
        (BeanReflector.<Response>getInstance(Response.class));
    return focus.chain(responseChannel);
  }

  @Override
  protected FetchTask task()
  { return new FetchTask();
  }

  protected class FetchTask
    extends ChainTask
  {
    @Override
    protected void work()
      throws InterruptedException
    {
      Response response;
      URI uri=uri();
      try
      { 
        response
          =newClient().executeRequest
            (method(),uri,contentType(),content(),headers());
      }
      catch (IOException x)
      { 
        addException
          (new ContextualException
            ("Error fetching "+uri,getDeclarationInfo(),x)
          );
        return;
      }

      responseChannel.push(response);
      try
      { 
        if (logResponse)
        { log.info("Response from "+uri+": "+response);
        }
        super.work();
      }
      finally
      { responseChannel.pop();
      }
    }
  }
}