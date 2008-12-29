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
package spiralcraft.net.server.echo;

import spiralcraft.common.LifecycleException;

import spiralcraft.net.server.ProtocolHandler;
import spiralcraft.net.server.ProtocolHandlerFactory;
import spiralcraft.net.server.Server;


/**
 * A simple server which echoes input to output
 */
public class EchoServer
  extends Server
  implements ProtocolHandlerFactory
{
  
  @Override
  public void start()
    throws LifecycleException
  { 
    setProtocolHandlerFactory(this);
    super.start();
  }

  public ProtocolHandler createProtocolHandler()
  { return new EchoProtocolHandler();
  }

  public void discardProtocolHandler(ProtocolHandler handler)
  {
  }

}
