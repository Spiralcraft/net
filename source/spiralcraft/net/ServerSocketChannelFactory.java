package spiralcraft.net;

import java.net.InetAddress;

import java.io.IOException;

import java.nio.channels.ServerSocketChannel;

public interface ServerSocketChannelFactory
  extends ServerSocketFactory
{
  ServerSocketChannel createServerSocketChannel(int port)
    throws IOException;

  ServerSocketChannel createServerSocketChannel(int port,int backlog)
    throws IOException;

  ServerSocketChannel createServerSocketChannel(int port,int backlog,InetAddress address)
    throws IOException;
}
