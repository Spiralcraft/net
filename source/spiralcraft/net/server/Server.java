//
// Copyright (c) 1998,2005 Michael Toth
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

import java.util.logging.Logger;
import java.util.logging.Level;

import spiralcraft.service.Service;

import spiralcraft.registry.RegistryNode;
import spiralcraft.registry.Registrant;

import spiralcraft.common.LifecycleException;

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
  implements Service,Registrant,ResourceFactory,ProtocolHandlerSupport
{
  
  private Endpoint[] _endpoints;
  private ConnectionQueue _queue=new ConnectionQueue();
  private Thread _handlerThread;
  private Logger _logger;
  private ThreadPool _threadPool=new ThreadPool();
  private Pool _protocolHandlerPool=new Pool();
  private ProtocolHandlerFactory _protocolHandlerFactory;
  private int _connectionCount;
  private int _activeConnectionCount;
  private int _uncaughtIOExceptionCount;
  private URI _traceUri;
  private int _traceCount;
  private int _readTimeoutMillis;
  private StandardChannelDispatcher _channelDispatcher;
  private RegistryNode _registryNode;
  
  private long _bytesRead;
  private long _bytesWritten;

  public void setTraceURI(URI uri)
  { _traceUri=uri;
  }

  public void register(RegistryNode node)
  { 
    _registryNode=node;
    _logger=node.findInstance(Logger.class);
    _threadPool.register(node.createChild("threadPool"));
    _protocolHandlerPool.register(node.createChild("protocolHandlerPool"));
  }

  public Logger getLogger()
  { return _logger;
  }

  /**
   * Service.getSelector
   */
  public Object getSelector()
  { return null;
  }

  /**
   * Service.providesInterface
   * @param serviceInterface 
   */
  public boolean providesInterface(Class<?> serviceInterface)
  { return false;
  }

  /**
   * Service.getInterface
   * @param serviceInterface 
   */
  public Object getInterface(Class<?> serviceInterface)
  { return null;
  }


  /**
   * Service.init
   */
  public void start()
    throws LifecycleException
  { 
    try
    { 
      _threadPool.start();
      _protocolHandlerPool.setResourceFactory(this);
      _protocolHandlerPool.start();
      _channelDispatcher=new StandardChannelDispatcher();
      _channelDispatcher.register(_registryNode.createChild("channelDispatcher"));
      

      bind();
      startHandler();
      _channelDispatcher.start();
      if (_logger!=null && _logger.isLoggable(Level.INFO))
      { _logger.info("Started");
      }
    }
    catch (IOException x)
    { throw new LifecycleException("Error binding endpoints",x);
    }
  }


  /**
   * Service.destroy
   */  
  @Override
  public void stop()
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
      if (_logger!=null && _logger.isLoggable(Level.SEVERE))
      { _logger.severe("Server interrupted");
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
      { 
        if (_logger!=null)
        { _logger.warning("Could not set read timeout to "+_readTimeoutMillis);
        }
      }
    }

    Connection serverConnection=new ServerConnection(this,connection);

    ProtocolHandler handler=(ProtocolHandler) _protocolHandlerPool.checkout();
    if (_logger!=null && _logger.isLoggable(Level.FINE))
    { _logger.fine("Dispatching connection "+serverConnection.toString());
    }
    handler.handleConnection(this,serverConnection); 
  }

  /**
   * ProtocolHandlerSupport.protocolFinished()
   */
  public void protocolFinished(ProtocolHandler handler)
  { 
    _activeConnectionCount--;
    if (_logger!=null && _logger.isLoggable(Level.FINE))
    { _logger.fine("Protocol finished "+handler.toString());
    }
    _protocolHandlerPool.checkin(handler);
  }

  /**
   * ProtocolHandlerSupport.runBlockingOperation(Runnable)
   */
  public void runBlockingOperation(Runnable runnable)
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
          if (_logger!=null)
          { _logger.warning("Cannot write to "+resource.toString());
          }
          return null;
        }
      }
      catch (UnresolvableURIException x)
      { 
        if (_logger!=null)
        { _logger.warning(_traceUri.toString()+" could not be resolved");
        }
      }
      catch (IOException x)
      {
        if (_logger!=null)
        { _logger.warning("Error writing to "+resource.toString()+":"+x.toString());
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
