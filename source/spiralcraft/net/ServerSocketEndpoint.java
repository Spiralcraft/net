package spiralcraft.net;

import spiralcraft.util.ArrayUtil;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.nio.channels.ServerSocketChannel;

import spiralcraft.registry.Registrant;
import spiralcraft.registry.RegistryNode;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An Endpoint implemented by a dedicated Thread which 
 *   accepts connections from a ServerSocket.
 */
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
  private Thread _listenerThread;
  private boolean _paused=true;

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
    startListening();
  }

  public synchronized void release()
  {
    stopListening();
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

  private synchronized void stopListening()
  { _paused=true;
  }

  private synchronized void startListening()
  {
    if (_listenerThread==null)
    { 
      _listenerThread
        =new Thread
          (new Runnable()
          {
            public void run()
            { accept();
            }
          }
          ,"Endpoint-"+(_address==null?"*:":_address.getHostAddress()+":")+_port 
          );
      _listenerThread.setPriority(Thread.MAX_PRIORITY);
      _listenerThread.setDaemon(true);
      _listenerThread.start();
    }
    
    _paused=false;
    notify();
  }

  private void accept()
  {
    while (true)
    {
      if (_paused)
      {
        synchronized (this)
        { 
          while (_paused)
          { 
            try
            { wait();
            }
            catch (InterruptedException x)
            {
              if (_logger!=null)
              { _logger.severe("ServerSocketEndpoint interrupted while paused");
              }
              return;
            }
          }
        }
      }

      try
      {
        Socket socket=_serverSocket.accept();

        if (_logger!=null && _logger.isLoggable(Level.FINE))
        { _logger.fine("Incoming connection from "+socket.getInetAddress().getHostAddress());
        }

        Connection connection=new SocketConnection(socket);
        ConnectionEvent event=new ConnectionEvent(connection);
        for (int i=0;i<_listeners.length;i++)
        { _listeners[i].connectionEstablished(event);
        }
      }
      catch (SocketException x)
      {
        if (!_serverSocket.isClosed())
        {
          if (_logger!=null && _logger.isLoggable(Level.WARNING))
          { _logger.warning("Exception while accepting: "+x.toString());
          }
          else
          { x.printStackTrace();
          }
        }
      }
      catch (IOException x)
      { 
        if (_logger!=null && _logger.isLoggable(Level.WARNING))
        { _logger.warning("Exception while accepting: "+x.toString());
        }
        else
        { x.printStackTrace();
        }
      }
      
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
    { _logger.info("Bound to "+(_address!=null?_address.toString():_serverSocket.getInetAddress().getHostAddress().toString())+":"+_port);
    }
  }

}
