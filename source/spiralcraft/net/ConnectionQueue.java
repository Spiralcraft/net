package spiralcraft.net;

import spiralcraft.util.SynchronizedQueue;

import java.io.IOException;

/**
 * A SynchronizedQueue which handles Connections
 */ 
public class ConnectionQueue
  extends SynchronizedQueue
  implements ConnectionListener
{

  /**
   * ConnectionListener.connectionAccepted
   */
  public void connectionAccepted(ConnectionEvent event)
  { 
    Connection connection=event.getConnection();
    try
    { 
      connection.addConnectionListener(this);
      add(connection);
    }
    catch (InterruptedException x)
    { 
      connection.removeConnectionListener(this);
      x.printStackTrace();
      try
      { event.getConnection().close();
      }
      catch (IOException y)
      { }
    }
  }

  /**
   * ConnectionListener.connectionEstablished
   */
  public void connectionEstablished(ConnectionEvent event)
  {
  }

  /**
   * ConnectionListener.connectionClosed
   */
  public void connectionClosed(ConnectionEvent event)
  { remove(event.getConnection());
  }

  public Connection nextConnection()
    throws InterruptedException
  {
    Connection connection=(Connection) next();
    connection.removeConnectionListener(this);
    return connection;
  }


}
