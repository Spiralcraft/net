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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.URI;

import spiralcraft.util.ArrayUtil;


/**
 * A Connection based on a Socket.
 */
public class SocketConnection
  implements Connection
{
  private final Socket _socket;
  
  private ConnectionListener[] _listeners
    =new ConnectionListener[0];
  
  private URI _localAddress;
  private URI _remoteAddress;
  
  private InputStream _in;
  private OutputStream _out;

  
  /**
   * Construct a blocking SocketConnection
   */
  public SocketConnection(Socket socket)
  { _socket=socket;  
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
  public boolean isSecure()
  { return false;
  }

  @Override
  public void setReadTimeoutMillis(int millis)
    throws IOException
  { _socket.setSoTimeout(millis);
  }

  @Override
  public URI getLocalAddress()
  {
    if (_localAddress==null)
    { 
      _localAddress
        =URI.create
          ("socket://"
          +_socket.getLocalAddress().getHostAddress()
          +":"+Integer.toString(_socket.getLocalPort())
          );
    }
    return _localAddress;
  }

  @Override
  public URI getRemoteAddress()
  {
    if (_remoteAddress==null)
    { 
      _remoteAddress
        =URI.create
          ("socket://"
          +_socket.getInetAddress().getHostAddress()
          +":"+Integer.toString(_socket.getPort())
          );
    }
    return _remoteAddress;
  }

  @Override
  public synchronized void close()
    throws IOException
  { 
    _socket.close();

    ConnectionEvent event=new ConnectionEvent(this);
    for (int i=0;i<_listeners.length;i++)
    { _listeners[i].connectionClosed(event);
    }
  }

  
  /**
   * Return a blocking InputStream
   */
  @Override
  public InputStream getInputStream()
    throws IOException
  { 
    if (_in==null)
    { _in=_socket.getInputStream();
    }
    return _in;
  }

  /**
   * Return a blocking OutputStream
   */
  @Override
  public OutputStream getOutputStream()
    throws IOException
  { 
    if (_out==null)
    { _out=_socket.getOutputStream();
    }
    return _out;
  }

  @Override
  public String toString()
  { 
    if (_socket.isConnected())
    {
      return "SocketConnection["
        +_socket.getInetAddress().getHostAddress()
        +"->"
        +_socket.getLocalAddress().getHostAddress()
        +"]"
        ;
    }
    else
    { return "SocketConnection[unconnected]";
    }
  }

}
