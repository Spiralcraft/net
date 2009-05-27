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
import java.io.InterruptedIOException;

import java.net.Socket;
import java.net.URI;

import spiralcraft.util.ArrayUtil;

import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;

import java.nio.ByteBuffer;

/**
 * A Connection based on a Socket.
 */
public class SocketConnection
  implements Connection,ChannelListener
{
  private final Socket _socket;
  private final SocketChannel _socketChannel;
  private final ChannelDispatcher _dispatcher;
  private final SelectionKey _key;
  private final Object _readLock;
  private final Object _writeLock;
  
  private ConnectionListener[] _listeners
    =new ConnectionListener[0];
  
  private URI _localAddress;
  private URI _remoteAddress;

  private boolean _readable;
  private boolean _writable;
  
  private InputStream _in;
  private OutputStream _out;
  
  /**
   * Construct a non-blocking SocketConnection
   */
  public SocketConnection(SocketChannel socketChannel,ChannelDispatcher dispatcher)
    throws IOException
  { 
    _socketChannel=socketChannel;
    _socketChannel.configureBlocking(false);
    _socket=socketChannel.socket();
    _dispatcher=dispatcher;
    _key=dispatcher.registerChannel(socketChannel,this);
    _readLock=new Object();
    _writeLock=new Object();
  }
  
  /**
   * Construct a blocking SocketConnection
   */
  public SocketConnection(Socket socket)
  { 
    _socketChannel=null;
    _key=null;
    _readLock=null;
    _writeLock=null;
    _socket=socket;
    _dispatcher=null;
    
  }

  public synchronized void addConnectionListener(ConnectionListener listener)
  { 
    if (!ArrayUtil.contains(_listeners,listener))
    { _listeners=ArrayUtil.append(_listeners,listener);
    }
  }

  public synchronized void removeConnectionListener(ConnectionListener listener)
  { _listeners=ArrayUtil.remove(_listeners,listener);
  }
  
  public void channelAccept(ChannelEvent event)
  { 
    throw new UnsupportedOperationException
      ("Connections cannot accept connections");
  }
  
  public synchronized void channelConnect(ChannelEvent event)
  { 
    ConnectionEvent connEvent=new ConnectionEvent(this);
    for (int i=0;i<_listeners.length;i++)
    { _listeners[i].connectionEstablished(connEvent);
    }
    _key.interestOps(_key.interestOps() & ~SelectionKey.OP_CONNECT);
    _in=new BlockingInputStream();
    _out=new BlockingOutputStream();
  }

  public void channelRead(ChannelEvent event)
  { 
    synchronized (_readLock)
    {
      if (!_readable)
      { 
        _readable=true;
        _key.interestOps(_key.interestOps() & ~SelectionKey.OP_READ);
        _readLock.notify();
      }
    }
  }
  
  public void channelWrite(ChannelEvent event)
  { 
    synchronized (_writeLock)
    {
      if (!_writable)
      { 
        _writable=true;
        _key.interestOps(_key.interestOps() & ~SelectionKey.OP_WRITE);
        _writeLock.notify();
      }
    }
  }
  
  public boolean isSecure()
  { return false;
  }

  public void setReadTimeoutMillis(int millis)
    throws IOException
  { _socket.setSoTimeout(millis);
  }

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

  public synchronized void close()
    throws IOException
  { 
    _socket.close();
    if (_socketChannel!=null)
    { _socketChannel.close();
    }
    
    synchronized (_readLock)
    { _readLock.notifyAll();
    }
    
    synchronized (_writeLock)
    { _writeLock.notifyAll();
    }

    ConnectionEvent event=new ConnectionEvent(this);
    for (int i=0;i<_listeners.length;i++)
    { _listeners[i].connectionClosed(event);
    }
  }

  
  /**
   * Return a blocking InputStream
   */
  public InputStream getInputStream()
    throws IOException
  { 
    if (_socketChannel==null)
    { return _socket.getInputStream();
    }
    else
    { return _in;
    }
  }

  /**
   * Return a blocking OutputStream
   */
  public OutputStream getOutputStream()
    throws IOException
  { 
    if (_socketChannel==null)
    { return _socket.getOutputStream();
    }
    else
    { return _out;
    }
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
  
  private void readStarved()
  {
    _readable=false;
    _key.interestOps(_key.interestOps() | SelectionKey.OP_READ);
    _dispatcher.wakeup();
  }
  
  private void writeStarved()
  {
    _writable=false;
    _key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
    _dispatcher.wakeup();
  }
  
  class BlockingInputStream
    extends InputStream
  {
    private boolean _eof=false;
    
    @Override
    public int read()
      throws IOException
    {
      byte[] buff=new byte[1];
      int ret=read(buff,0,1);
      if (ret==-1)
      { return -1;
      }
      else
      { return buff[0];
      }
    }

    @Override
    public int read(byte[] buff)
      throws IOException
    { return read(buff,0,buff.length);
    }
    
    @Override
    public int read(byte[] buff,int start,int len)
      throws IOException
    { 
      synchronized (_readLock)
      {
        if (!_readable)
        { 
          try
          { _readLock.wait();
          }
          catch (InterruptedException x)
          { throw new InterruptedIOException();
          }
        }
        
        if (!_socketChannel.isOpen())
        { 
          if (!_eof)
          { 
            _eof=true;
            return -1;
          }
          else
          { throw new IOException("EOF");
          }
        }
        
        if (!_readable)
        { throw new IllegalStateException("Notified when not closed and not readable");
        }
        
        ByteBuffer bbuff=ByteBuffer.wrap(buff,start,len);
        int ret=_socketChannel.read(bbuff);
        if (ret==-1)
        { 
          if (!_eof)
          { 
            _eof=true;
            return -1;
          }
          else
          { throw new IOException("EOF");
          }
        }
        else
        { 
          if (ret< len)
          { readStarved();
          }
          return ret;
        }
      }
    }

  }

  class BlockingOutputStream
    extends OutputStream
  {
    @Override
    public void write(int val)
      throws IOException
    {
      byte[] buff=new byte[] {(byte) val};
      write(buff,0,1);
    }

    @Override
    public void write(byte[] buff)
      throws IOException
    { write(buff,0,buff.length);
    }
    
    @Override
    public void write(byte[] buff,int start,int len)
      throws IOException
    { 
      int count=0;
      while (count<len)
      {
        int remain=len-count;
        int offset=start+count;
        synchronized (_writeLock)
        {
          if (!_writable)
          { 
            try
            { _writeLock.wait();
            }
            catch (InterruptedException x)
            { throw new InterruptedIOException();
            }
          }
          
          if (!_socketChannel.isOpen())
          { throw new IOException("Socket closed");
          }
          
          if (!_writable)
          { throw new IllegalStateException("Notified when not closed and not writable");
          }
          
          ByteBuffer bbuff=ByteBuffer.wrap(buff,offset,remain);
          int writeCount=_socketChannel.write(bbuff);
          if (writeCount<remain)
          { writeStarved();
          }
          count+=writeCount;
        }
      }
    }

  }
}
