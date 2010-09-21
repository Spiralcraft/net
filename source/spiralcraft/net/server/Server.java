//
// Copyright (c) 1998,2009 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net.server;


import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;


import spiralcraft.service.Service;


import spiralcraft.app.spi.AbstractComponent;
import spiralcraft.common.LifecycleException;

import spiralcraft.log.Level;
import spiralcraft.net.io.Connection;
import spiralcraft.net.io.ConnectionQueue;
import spiralcraft.net.io.Endpoint;
import spiralcraft.net.io.StandardChannelDispatcher;

import spiralcraft.pool.ThreadPool;
import spiralcraft.pool.Pool;
import spiralcraft.pool.ResourceFactory;

import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;


/**
 * Generic Server framework. Accepts connections into a Queue,
 *   and dispatches IO events.
 */
public class Server
  extends AbstractComponent
  implements ResourceFactory<ProtocolHandler>,ProtocolHandlerSupport,Service
{
  
  private Endpoint[] _endpoints;
  private ConnectionQueue _queue=new ConnectionQueue();
  private Thread _handlerThread;
  private ThreadPool _threadPool=new ThreadPool();
  private Pool<ProtocolHandler> _protocolHandlerPool
    =new Pool<ProtocolHandler>();
  private ProtocolHandlerFactory _protocolHandlerFactory;
  private int _connectionCount;
  private int _activeConnectionCount;
  private int _uncaughtIOExceptionCount;
  private URI _traceUri;
  private int _traceCount;
  private int _readTimeoutMillis;
  private StandardChannelDispatcher _channelDispatcher;
  
  private long _bytesRead;
  private long _bytesWritten;

  public void setTraceURI(URI uri)
  { _traceUri=uri;
  }



  /**
   * Service.start()
   */
  @Override
  public void start()
    throws LifecycleException
  { 
    try
    { 
      _threadPool.start();
      _protocolHandlerPool.setResourceFactory(this);
      _protocolHandlerPool.start();
      _channelDispatcher=new StandardChannelDispatcher();
      

      bind();
      startHandler();
      _channelDispatcher.start();
      if (log.canLog(Level.INFO))
      { log.info("Started");
      }
    }
    catch (IOException x)
    { throw new LifecycleException("Error binding endpoints",x);
    }
  }


  /**
   * Service.stop()
   */  
  @Override
  public void stop()
    throws LifecycleException
  { 
    release();
    _channelDispatcher.stop();
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

  public void setReadTimeoutMillis(int millis)
  { _readTimeoutMillis=millis;
  }

  /**
   * Create a new ProtocolHandler for the pool.
   */
  @Override
  public ProtocolHandler createResource()
  { return _protocolHandlerFactory.createProtocolHandler();
  }

  /**
   * Discard a resource when no longer needed by the Pool.
   */
  @Override
  public void discardResource(ProtocolHandler resource)
  { _protocolHandlerFactory.discardProtocolHandler(resource);
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
          _endpoints[i].init();
          _endpoints[i].addConnectionListener(_queue);

          if (_endpoints[i].supportsNonBlockingIO())
          { _endpoints[i].bind(_channelDispatcher);
          }
          else
          { _endpoints[i].bind();
          }
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

  private synchronized void startHandler()
  {
    if (_handlerThread==null)
    { 
      _handlerThread
        =new Thread
          (new Runnable()
          {
            @Override
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
   *   assign a ProtocolHandler from a pool to handle the 
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
      if (log.canLog(Level.SEVERE))
      { log.severe("Server interrupted");
      }
      return;
    }
  }

  /**
   * Wrap the Connection with a ServerConnection to provide
   *   instrumentation (for monitoring, accounting, debugging) 
   *   and control (flow, quota, security) on a server-wide basis.
   *
   * Pass the wrapped connection off to a ProtocolHandler.
   */
  private void handleConnection(Connection connection)
  {
    _connectionCount++;
    _activeConnectionCount++;

    if (_readTimeoutMillis>0)
    { 
      try
      { connection.setReadTimeoutMillis(_readTimeoutMillis);
      }
      catch (IOException x)
      { log.warning("Could not set read timeout to "+_readTimeoutMillis);
      }
    }

    Connection serverConnection=new ServerConnection(this,connection);

    try
    {
      ProtocolHandler handler=_protocolHandlerPool.checkout();
      if (log.canLog(Level.FINE))
      { log.fine("Dispatching connection "+serverConnection.toString());
      }
      handler.handleConnection(this,serverConnection); 
    }
    catch (InterruptedException x)
    { 
    }
  }

  /**
   * ProtocolHandlerSupport.protocolFinished()
   */
  @Override
  public void protocolFinished(ProtocolHandler handler)
  { 
    _activeConnectionCount--;
    if (log.canLog(Level.FINE))
    { log.fine("Protocol finished "+handler.toString());
    }
    _protocolHandlerPool.checkin(handler);
  }

  /**
   * ProtocolHandlerSupport.runBlockingOperation(Runnable)
   */
  @Override
  public void runBlockingOperation(Runnable runnable)
    throws InterruptedException
  { _threadPool.run(runnable);
  }

  /**
   * Create a new trace stream
   */
  public OutputStream createTraceStream()
  { 
    if (_traceUri!=null)
    { 
      Resource resource=null;
      try
      {
        resource
          =Resolver.getInstance().resolve
            (_traceUri.resolve("spiralcraft.server.trace-"+Integer.toString(_traceCount++))
            );
        
        OutputStream out=resource.getOutputStream();
        if (out!=null)
        { return out;
        }
        else
        {
          log.warning("Cannot write to "+resource.toString());
          return null;
        }
      }
      catch (UnresolvableURIException x)
      { log.warning(_traceUri.toString()+" could not be resolved");
      }
      catch (IOException x)
      {
        if (log!=null)
        { log.warning("Error writing to "+resource.toString()+":"+x.toString());
        }
      }
    }
    return null;
  }

  /**
   * Indicate that the traceStream is finished
   */
  public void traceStreamFinished(OutputStream traceStream)
  {
    if (traceStream!=null)
    { 
      try
      { traceStream.close();
      }
      catch (IOException x)
      { }
    }
  }

  public void uncaughtIOException()
  { _uncaughtIOExceptionCount++;
  }

  /**
   * Called by the ServerInputStream when data is read
   */
  public synchronized void countBytesRead(long bytes)
  { _bytesRead+=bytes;
  }

  /**
   * Called by the ServerInputStream when data is written
   */
  public synchronized void countBytesWritten(long bytes)
  { _bytesWritten+=bytes;
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
