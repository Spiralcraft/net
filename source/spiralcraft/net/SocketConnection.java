package spiralcraft.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.URI;

/**
 * A Connection based on a Socket.
 */
public class SocketConnection
  implements Connection
{
  private final Socket _socket;
  private URI _localAddress;
  private URI _remoteAddress;
  
  public SocketConnection(Socket socket)
  { _socket=socket;
  }

  public boolean isSecure()
  { return false;
  }

  public void setReadTimeoutMillis(int millis)
    throws IOException
  { _socket.setSoTimeout(millis);
  }

  public void addConnectionListener(ConnectionListener listener)
  {
  }
  
  public void removeConnectionListener(ConnectionListener listener)
  {
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
