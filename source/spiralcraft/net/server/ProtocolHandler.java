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
package spiralcraft.net.server;

import spiralcraft.net.io.Connection;

/**
 * Runs a server protocol for a single connection.
 */
public interface ProtocolHandler
{

  /**
   * Handle a connection. 
   *
   * The implementation should call ProtocolHandlerSupport.protocolFinished()
   *   when the protocol is complete so the ProtocolHandler can be re-used.
   */
  void handleConnection(ProtocolHandlerSupport support,Connection connection);

}
