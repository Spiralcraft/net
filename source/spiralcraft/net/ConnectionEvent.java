package spiralcraft.net;

import java.util.EventObject;

public class ConnectionEvent
  extends EventObject
{

  private Connection _connection;

  public ConnectionEvent(Connection connection)
  { 
    super(connection);
    _connection=connection;
  }
  
  public Connection getConnection()
  { return _connection;
  }
  
}
