//
// Copyright (c) 2015 Michael Toth
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
package spiralcraft.net.http.client;

import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;


/**
 * An HTTP connection
 */
public class HttpsConnection
  extends HttpConnection
{
  /**
   * Create a new connection to the specified host/port  
   * @param socket
   */
  public HttpsConnection(String hostname,int port)
    throws UnknownHostException
  { 
    super(hostname,port);
    defaultPort=443;
  }
  
  protected SocketFactory getSocketFactory()
  { return SSLSocketFactory.getDefault();
  }

}