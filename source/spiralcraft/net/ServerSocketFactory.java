package spiralcraft.net;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.IOException;

public interface ServerSocketFactory
{
  ServerSocket createServerSocket(int port)
    throws IOException;

  ServerSocket createServerSocket(int port,int backlog)
    throws IOException;

  ServerSocket createServerSocket(int port,int backlog,InetAddress address)
    throws IOException;
}
