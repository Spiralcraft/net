package spiralcraft.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;

/**
 * A Connection based on a Socket.
 */
public class SocketConnection
  implements Connection
{
  private final Socket _socket;

  public SocketConnection(Socket socket)
  { _socket=socket;
  }

  public void addConnectionListener(ConnectionListener listener)
  {
  }
  
  public void removeConnectionListener(ConnectionListener listener)
  {
  }

  public void close()
    throws IOException
  { _socket.close();
  }

  public InputStream getInputStream()
    throws IOException
  { return _socket.getInputStream();
  }

  public OutputStream getOutputStream()
    throws IOException
  { return _socket.getOutputStream();
  }

  public String toString()
  { 
    return "SocketConnection["
      +_socket.getInetAddress().getHostAddress()
      +"->"
      +_socket.getLocalAddress().getHostAddress()
      +"]"
      ;
  }
}
