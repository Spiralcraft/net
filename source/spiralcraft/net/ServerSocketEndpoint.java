package spiralcraft.net;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;


import spiralcraft.registry.Registrant;
import spiralcraft.registry.RegistryNode;

import java.util.logging.Logger;
import java.util.logging.Level;

public class ServerSocketEndpoint
  implements Endpoint,Registrant
{
  private ConnectionListener[] _listeners=new ConnectionListener[0];
  private int _port;
  private String _interfaceName;
  private InetAddress _address;
  private int _listenBacklog;
  private ServerSocketFactory _factory;  
  private ServerSocket _serverSocket;
  private Logger _logger;

  public void register(RegistryNode node)
  { _logger=(Logger) node.findInstance(Logger.class);
  }

  public void setPort(int val)
  { _port=val;
  }

  public void setInterfaceName(String val)
  { _interfaceName=val;
  }

  public void setListenBacklog(int val)
  { _listenBacklog=val;
  }

  public void setServerSocketFactory(ServerSocketFactory factory)
  { _factory=factory;
  }

  public synchronized void addConnectionListener(ConnectionListener listener)
  { 
    if (!ArrayUtil.contains(_listeners,listener))
    { _listeners=(ConnectionListener[]) ArrayUtil.append(_listeners,listener);
    }
  }

  public synchronized void removeConnectionListener(ConnectionListener listener)
  { _listeners=(ConnectionListener[]) ArrayUtil.remove(_listeners,listener);
  }

  public synchronized void bind()
    throws IOException
  {
    resolveInterface();
    if (_factory==null)
    { _factory=new StandardServerSocketFactory();
    }
    bindSocket();
  }

  public synchronized void release()
  {
    try
    {
      if (_serverSocket!=null)
      { _serverSocket.close();
      }
    }
    catch (IOException x)
    { x.printStackTrace();
    }
  }

  private void resolveInterface()
    throws IOException
  {
    try
    {
      if (_interfaceName!=null)
      { _address=InetAddress.getByName(_interfaceName);
      }
    }
    catch (UnknownHostException x)
    { 
      throw new IOException
        ("Exception binding Endpoint to "
        +_interfaceName+":"+_port+": "+x.toString()
        );
    }

  }

  private void bindSocket()
    throws IOException
  {
    if (_address!=null)
    { _serverSocket=_factory.createServerSocket(_port,_listenBacklog,_address);
    }
    else
    { _serverSocket=_factory.createServerSocket(_port,_listenBacklog);
    }

    if (_logger!=null && _logger.isLoggable(Level.INFO))
    { _logger.info("Bound to "+(_address!=null?_address.toString():"*")+":"+_port);
    }
  }

}
