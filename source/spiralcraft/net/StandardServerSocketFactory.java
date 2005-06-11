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
package spiralcraft.net;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.channels.ServerSocketChannel;

import java.io.IOException;

/**
 * Creates standard blocking and non-blocking server sockets.
 */
public class StandardServerSocketFactory
  implements ServerSocketChannelFactory
{
  public ServerSocket createServerSocket(int port)
    throws IOException
  { return new ServerSocket(port);
  }

  public ServerSocket createServerSocket(int port,int backlog)
    throws IOException
  { return new ServerSocket(port,backlog);
  }

  public ServerSocket createServerSocket(int port,int backlog,InetAddress address)
    throws IOException
  { return new ServerSocket(port,backlog,address);    
  }

  public ServerSocketChannel createServerSocketChannel(int port)
    throws IOException
  { 
    ServerSocketChannel channel=ServerSocketChannel.open();
    InetSocketAddress addr=new InetSocketAddress(InetAddress.getLocalHost(),port);
    channel.configureBlocking(false);
    channel.socket().bind(addr);
    return channel;
  }

  public ServerSocketChannel createServerSocketChannel(int port,int backlog)
    throws IOException
  { 
    ServerSocketChannel channel=ServerSocketChannel.open();
    InetSocketAddress addr=new InetSocketAddress(InetAddress.getLocalHost(),port);
    channel.configureBlocking(false);
    channel.socket().bind(addr,backlog);
    return channel;
  }


  public ServerSocketChannel createServerSocketChannel
    (int port
    ,int backlog
    ,InetAddress address
    )
    throws IOException
  {
    ServerSocketChannel channel=ServerSocketChannel.open();
    InetSocketAddress addr=new InetSocketAddress(address,port);
    channel.configureBlocking(false);
    channel.socket().bind(addr,backlog);
    return channel;    
  }

}
