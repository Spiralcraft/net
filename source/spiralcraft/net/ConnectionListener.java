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

/**
 * Receives a ConnectionEvent when a Connection is established or
 *   closed.
 */
public interface ConnectionListener
{

  /**
   * Called when a Connection is accepted by an Endpoint.
   * 
   * This method may be called on a high priority Thread which may
   *   be responsible for handling external events. Implementations
   *   must return from this method without undue delay. If the
   *   Connection cannot be handled without blocking, it should
   *   be enqueued for further processing by the implementation.
   */
  public void connectionAccepted(ConnectionEvent event);
  
  /**
   * Called when a Connection is established.
   * 
   * This method may be called on a high priority Thread which may
   *   be responsible for handling external events. Implementations
   *   must return from this method without undue delay. If the
   *   Connection cannot be handled without blocking, it should
   *   be enqueued for further processing by the implementation.
   */
  public void connectionEstablished(ConnectionEvent event);

  /**
   * Called when a Connection is closed.
   *
   * This method may be called on a high priority Thread which may
   *   be responsible for handling external events. Implementations
   *   must return from this method without undue delay. If the
   *   Connection cannot be cleaned up without blocking, it should
   *   be enqueued for further processing by the implementation.
   */
  public void connectionClosed(ConnectionEvent event);
  
  
}
