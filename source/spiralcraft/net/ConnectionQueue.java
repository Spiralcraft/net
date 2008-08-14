//
// Copyright (c) 1998,2005 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net;

import spiralcraft.util.SynchronizedQueue;

import java.io.IOException;

/**
 * A SynchronizedQueue which handles Connections
 */ 
public class ConnectionQueue
  extends SynchronizedQueue<Connection>
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
    Connection connection=next();
    connection.removeConnectionListener(this);
    return connection;
  }


}
