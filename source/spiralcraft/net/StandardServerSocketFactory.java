package spiralcraft.net;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.channels.ServerSocketChannel;

import java.io.IOException;

public class StandardServerSocketFactory
  implements ServerSocketChannelFactory
{
  public ServerSocket createServerSocket(int port)
    throws IOException
  { return createServerSocketChannel(port).socket();
  }

  public ServerSocketChannel createServerSocketChannel(int port)
    throws IOException
  { 
    ServerSocketChannel channel=ServerSocketChannel.open();
    InetSocketAddress addr=new InetSocketAddress(InetAddress.getLocalHost(),port);
    channel.socket().bind(addr);
    return channel;
  }

  public ServerSocket createServerSocket(int port,int backlog)
    throws IOException
  { return createServerSocketChannel(port,backlog).socket();
  }

  public ServerSocketChannel createServerSocketChannel(int port,int backlog)
    throws IOException
  { 
    ServerSocketChannel channel=ServerSocketChannel.open();
    InetSocketAddress addr=new InetSocketAddress(InetAddress.getLocalHost(),port);
    channel.socket().bind(addr,backlog);
    return channel;
  }

  public ServerSocket createServerSocket(int port,int backlog,InetAddress address)
    throws IOException
  { return createServerSocketChannel(port,backlog,address).socket();
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
    channel.socket().bind(addr,backlog);
    return channel;    
  }

}
