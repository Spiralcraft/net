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
package spiralcraft.net.io;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An Endpoint implemented by a dedicated Thread which 
 *   accepts connections from a ServerSocket.
 */
public class ServerSocketEndpoint
  implements Endpoint,ChannelListener
{
  private ConnectionListener[] _listeners=new ConnectionListener[0];
  private int _port;
  private String _interfaceName;
  private InetAddress _address;
  private int _listenBacklog;
  private ServerSocketFactory _factory;  
  private ServerSocketChannelFactory _channelFactory;  
  private ServerSocket _serverSocket;
  private ServerSocketChannel _serverSocketChannel;
  private Logger _logger;
  private Thread _listenerThread;
  private boolean _paused=true;
  private boolean _running=false;
  private ChannelDispatcher _dispatcher;
  


  public void setPort(int val)
  { 
    assertNotRunning();
    _port=val;
  }

  public void setInterfaceName(String val)
  { 
    assertNotRunning();
    _interfaceName=val;
  }

  public void setListenBacklog(int val)
  { 
    assertNotRunning();
    _listenBacklog=val;
  }

  public void setServerSocketFactory(ServerSocketFactory factory)
  { 
    assertNotRunning();
    _factory=factory;
    if (_factory instanceof ServerSocketChannelFactory)
    { _channelFactory=(ServerSocketChannelFactory) _factory;
    }
  }

  @Override
  public synchronized void addConnectionListener(ConnectionListener listener)
  { 
    if (!ArrayUtil.contains(_listeners,listener))
    { _listeners=ArrayUtil.append(_listeners,listener);
    }
  }

  @Override
  public synchronized void removeConnectionListener(ConnectionListener listener)
  { _listeners=ArrayUtil.remove(_listeners,listener);
  }

  @Override
  public void init()
  {
    if (_factory==null)
    { setServerSocketFactory(new StandardServerSocketFactory());
    }
  }

  /**
   * Indicate whether the Endpoint supports non-blocking IO
   */
  @Override
  public boolean supportsNonBlockingIO()
  { 
    if (_factory==null)
    { throw new IllegalStateException("Not initialized");
    }
    return _channelFactory!=null;
  }
  
  /**
   * Bind for non-blocking operation
   */
  @Override
  public synchronized void bind(ChannelDispatcher dispatcher)
    throws IOException
  {
    resolveInterface();
    if (_factory==null)
    { throw new IllegalStateException("Not initialized");
    }
    bindSocketNonBlocking();
    _dispatcher=dispatcher;
    _dispatcher.registerChannel(_serverSocketChannel,this);
    _running=true;
  }
  
  /**
   * Bind for blocking operation
   */
  @Override
  public synchronized void bind()
    throws IOException
  {
    resolveInterface();
    if (_factory==null)
    { throw new IllegalStateException("Not initialized");
    }
    bindSocketBlocking();
    startListening();
  }

  /**
   * Unbind and release all resources
   */
  @Override
  public synchronized void release()
  {
    stopListening();
    _running=false;
    try
    {
      if (_serverSocket!=null)
      { _serverSocket.close();
      }
    }
    catch (IOException x)
    { x.printStackTrace();
    }
  }

  private synchronized void stopListening()
  { _paused=true;
  }

  /**
   * Start listening in blocking mode by running a dedicated thread
   *   to accept connections and hand them off to the ConnectionListeners.
   */
  private synchronized void startListening()
  {
    if (_listenerThread==null)
    { 
      _listenerThread
        =new Thread
          (new Runnable()
          {
            @Override
            public void run()
            { acceptUntilReleased();
            }
          }
          ,"Endpoint-"+(_address==null?"*:":_address.getHostAddress()+":")+_port 
          );
      _listenerThread.setPriority(Thread.MAX_PRIORITY);
      _listenerThread.setDaemon(true);
      _listenerThread.start();
    }
    
    _paused=false;
    notify();
  }

  @Override
  public void channelAccept(ChannelEvent event)
  { accept();
  }

  @Override
  public void channelRead(ChannelEvent event)
  {
  }
  
  @Override
  public void channelWrite(ChannelEvent event)
  {
  }
  
  @Override
  public void channelConnect(ChannelEvent event)
  {
  }
  
  /**
   * Accept and dispatch an incoming connection.
   *
   * If the Endpoint is in blocking mode, this method will block until a
   *   connection is available. 
   *
   * If the Endpoint is in non-blocking mode, this method will accept a
   *   waiting connection.
   */
  private void accept()
  {
    try
    {
      if (_serverSocketChannel!=null)
      { 
        SocketChannel socketChannel=_serverSocketChannel.accept();
        if (socketChannel!=null)
        {
          if (_logger!=null && _logger.isLoggable(Level.FINE))
          { 
            _logger.fine("Incoming connection from "
              +socketChannel.socket().getInetAddress().getHostAddress());
          }
          
          Connection connection=new SocketChannelConnection(socketChannel,_dispatcher);
          fireConnectionAccepted(connection);
        }
        else
        {
          if (_logger!=null && _logger.isLoggable(Level.FINE))
          { _logger.fine("accept() returned null");
          }
        }
        
      }
      else
      {
        
        Socket socket=_serverSocket.accept();
  
        if (_logger!=null && _logger.isLoggable(Level.FINE))
        { _logger.fine("Incoming connection from "+socket.getInetAddress().getHostAddress());
        }
        
        Connection connection=new SocketChannelConnection(socket);
        fireConnectionAccepted(connection);
      }
    }
    catch (SocketException x)
    {
      if (!_serverSocket.isClosed())
      {
        if (_logger!=null && _logger.isLoggable(Level.WARNING))
        { _logger.warning("Exception while accepting: "+x.toString());
        }
        else
        { x.printStackTrace();
        }
      }
    }
    catch (IOException x)
    { 
      if (_logger!=null && _logger.isLoggable(Level.WARNING))
      { _logger.warning("Exception while accepting: "+x.toString());
      }
      else
      { x.printStackTrace();
      }
    }
  }

  private void fireConnectionAccepted(Connection connection)
  {
    ConnectionEvent event=new ConnectionEvent(connection);
    for (int i=0;i<_listeners.length;i++)
    { _listeners[i].connectionAccepted(event);
    }
  }
  
  /**
   * Accepts connections in blocking mode
   */
  private void acceptUntilReleased()
  {
    _running=true;
    while (true)
    {
      if (_paused)
      {
        synchronized (this)
        { 
          while (_paused)
          { 
            if (!_running)
            { return;
            }
            
            try
            { wait();
            }
            catch (InterruptedException x)
            {
              if (_logger!=null)
              { _logger.severe("ServerSocketEndpoint interrupted while paused");
              }
              _running=false;
              return;
            }
          }
        }
      }
      accept();
      
    }
  }

  private void resolveInterface()
    throws IOException
  {
    try
    {
      if (_interfaceName!=null)
      { _address=InetAddress.getByName(_interfaceName);
      }
    }
    catch (UnknownHostException x)
    { 
      throw new IOException
        ("Exception binding Endpoint to "
        +_interfaceName+":"+_port+": "+x.toString()
        );
    }

  }

  private void bindSocketBlocking()
    throws IOException
  {
    if (_address!=null)
    { _serverSocket=_factory.createServerSocket(_port,_listenBacklog,_address);
    }
    else
    { _serverSocket=_factory.createServerSocket(_port,_listenBacklog);
    }

    if (_logger!=null && _logger.isLoggable(Level.INFO))
    { 
      _logger.info
        ("Bound to "
        +(_address!=null
          ?_address.toString()
          :_serverSocket.getInetAddress().getHostAddress().toString()
         )
        +":"+_port
        );
    }
  }

  /**
   * Bind in non-blocking mode 
   */
  private void bindSocketNonBlocking()
    throws IOException
  {
    if (_channelFactory==null)
    { throw new IOException("Non-blocking operation not supported");
    }
    
    if (_address!=null)
    { 
      _serverSocketChannel
        =_channelFactory.createServerSocketChannel
          (_port,_listenBacklog,_address);
    }
    else
    { 
      _serverSocketChannel
        =_channelFactory.createServerSocketChannel
          (_port,_listenBacklog);
    }
    _serverSocket=_serverSocketChannel.socket();

    if (_logger!=null && _logger.isLoggable(Level.INFO))
    { 
      _logger.info
        ("Bound to "
        +(_address!=null
          ?_address.toString()
          :_serverSocket.getInetAddress().getHostAddress().toString()
         )
        +":"+_port
        );
    }
  }

  private void assertNotRunning()
  {
    if (_running)
    { throw new IllegalStateException("ServerSocketEndpoint is running");
    }
  }
}
