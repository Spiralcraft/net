package spiralcraft.net;

import java.net.InetAddress;

import java.io.IOException;

import java.nio.channels.ServerSocketChannel;

/**
 * Interface from which components obtain non blocking ServerSocketChannels
 *   without regard to socket implementation.
 *
 * All ServerSocketChannelFactories must implement the ServerSocketFactory to
 *   support standard blocking operation.
 */
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
