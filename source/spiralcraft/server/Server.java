package spiralcraft.server;

import spiralcraft.service.Service;
import spiralcraft.service.ServiceResolver;
import spiralcraft.service.ServiceException;

import spiralcraft.registry.RegistryNode;
import spiralcraft.registry.Registrant;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import spiralcraft.net.Connection;
import spiralcraft.net.ConnectionQueue;
import spiralcraft.net.Endpoint;

import spiralcraft.pool.ThreadPool;
import spiralcraft.pool.Pool;
import spiralcraft.pool.ResourceFactory;

/**
 * Generic Server framework. Accepts connections into a Queue,
 *   and dispatches IO events.
 */
public class Server
  implements Service,Registrant,ResourceFactory,ProtocolHandlerSupport
{
  
  private Endpoint[] _endpoints;
  private ConnectionQueue _queue=new ConnectionQueue();
  private Thread _handlerThread;
  private Logger _logger;
  private ThreadPool _threadPool=new ThreadPool();
  private Pool _protocolHandlerPool=new Pool();
  private ProtocolHandlerFactory _protocolHandlerFactory;

  public void register(RegistryNode node)
  { 
    _logger=(Logger) node.findInstance(Logger.class);
    _threadPool.register(node.createChild("threadPool"));
    _protocolHandlerPool.register(node.createChild("protocolHandlerPool"));
  }

  /**
   * Service.getSelector
   */
  public Object getSelector()
  { return null;
  }

  /**
   * Service.providesInterface
   */
  public boolean providesInterface(Class serviceInterface)
  { return false;
  }

  /**
   * Service.getInterface
   */
  public Object getInterface(Class serviceInterface)
  { return null;
  }


  /**
   * Service.init
   */
  public void init(ServiceResolver resolver)
    throws ServiceException
  { 
    try
    { 
      _threadPool.init();
      _protocolHandlerPool.setResourceFactory(this);
      _protocolHandlerPool.init();

      bind();
      start();
      if (_logger!=null && _logger.isLoggable(Level.INFO))
      { _logger.info("Started");
      }
    }
    catch (IOException x)
    { throw new ServiceException("Error binding endpoints",x);
    }
  }


  /**
   * Service.destroy
   */  
  public void destroy()
  { 
    release();
    _threadPool.stop();
    _protocolHandlerPool.stop();
  }

  /**
   * Install the Endpoints, which will feed incoming connections
   */
  public void setEndpoints(Endpoint[] val)
  { _endpoints=val;
  }

  public void setProtocolHandlerFactory(ProtocolHandlerFactory factory)
  { _protocolHandlerFactory=factory;
  }

  /**
   * Create a new ProtocolHandler for the pool.
   */
  public Object createResource()
  { return _protocolHandlerFactory.createProtocolHandler();
  }

  /**
   * Discard a resource when no longer needed by the Pool.
   */
  public void discardResource(Object resource)
  { _protocolHandlerFactory.discardProtocolHandler((ProtocolHandler) resource);
  }

  /**
   * Bind the endpoints and start accepting connections.
   */
  private void bind()
    throws IOException
  { 
    if (_endpoints!=null)
    {
      for (int i=0;i<_endpoints.length;i++)
      { 
        try
        { 
          _endpoints[i].addConnectionListener(_queue);
          _endpoints[i].bind();
        }
        catch (IOException x)
        {
          for (int j=i-1;j<=0;j--)
          { 
            _endpoints[j].release();
            _endpoints[j].removeConnectionListener(_queue);
          }
          throw x;
        }
      }
    }
  }

  private synchronized void start()
  {
    if (_handlerThread==null)
    { 
      _handlerThread
        =new Thread
          (new Runnable()
          {
            public void run()
            { handleConnections();
            }
          }
          ,"Server" 
          );
      _handlerThread.setDaemon(true);
      _handlerThread.start();
    }
  }

  /**
   * Remove connections from the incoming Queue and
   *   assign a Thread from a pool to handle the 
   *   connection.
   */
  private void handleConnections()
  {
    try
    {
      while (true)
      { 
        Connection connection=_queue.nextConnection();
        handleConnection(connection);
      }
    }
    catch (InterruptedException x)
    { 
      if (_logger!=null && _logger.isLoggable(Level.SEVERE))
      { _logger.severe("Server interrupted");
      }
      return;
    }
  }

  private void handleConnection(Connection connection)
  {
    ProtocolHandler handler=(ProtocolHandler) _protocolHandlerPool.checkout();
    if (_logger!=null && _logger.isLoggable(Level.FINE))
    { _logger.fine("Dispatching connection "+connection.toString());
    }
    handler.handleConnection(this,connection); 
  }

  /**
   * ProtocolHandlerSupport.protocolFinished()
   */
  public void protocolFinished(ProtocolHandler handler)
  { 
    if (_logger!=null && _logger.isLoggable(Level.FINE))
    { _logger.fine("Protocol finished "+handler.toString());
    }
    _protocolHandlerPool.checkin(handler);
  }

  /**
   * ProtocolHandlerSupport.runBlockingOperation(Runnable runnable)
   */
  public void runBlockingOperation(Runnable runnable)
  { _threadPool.run(runnable);
  }

  /**
   * Release the endpoints and stop accepting connections.
   */
  private void release()
  {
    if (_endpoints!=null)
    {
      for (int i=0;i<_endpoints.length;i++)
      { _endpoints[i].release();
      }
    }
  }
}
