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

import java.net.InetAddress;

import java.io.IOException;

import java.nio.channels.ServerSocketChannel;

/**
 * Interface from which components obtain non blocking ServerSocketChannels
 *   without regard to socket implementation.
 *
 * All ServerSocketChannelFactories must implement the ServerSocketFactory to
 *   support standard blocking operation.
 */
public interface ServerSocketChannelFactory
  extends ServerSocketFactory
{
  ServerSocketChannel createServerSocketChannel(int port)
    throws IOException;

  ServerSocketChannel createServerSocketChannel(int port,int backlog)
    throws IOException;

  ServerSocketChannel createServerSocketChannel(int port,int backlog,InetAddress address)
    throws IOException;
}
