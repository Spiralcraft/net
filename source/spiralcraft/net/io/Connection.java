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
package spiralcraft.net.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

/**
 * A bidirectional network Connection.
 */
public interface Connection
{

  /**
   * Add a ConnectionListener to be notified when this Connection
   *   is closed.
   */
  void addConnectionListener(ConnectionListener listener);
  
  /**
   * Remove a ConnectionListener
   */
  void removeConnectionListener(ConnectionListener listener);

  /**
   * Specify the maximum amount of time the connection will block
   *   waiting for data.
   */
  void setReadTimeoutMillis(int millis)
    throws IOException;

  void close()
    throws IOException;

  InputStream getInputStream()
    throws IOException;

  OutputStream getOutputStream()
    throws IOException;

  /**
   * Indicate whether data is transferred over
   *   this connection using a secure transport 
   *   protocol.
   */
  boolean isSecure();

  /**
   * Return a URI which identifies the remote endpoint
   *   of this connection.
   */
  URI getRemoteAddress();

  /**
   * Return a URI which identifies the local endpoint
   *   of this connection
   */
  URI getLocalAddress();
}
