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
