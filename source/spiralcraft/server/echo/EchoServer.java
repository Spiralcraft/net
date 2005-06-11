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
package spiralcraft.server.echo;

import spiralcraft.server.Server;
import spiralcraft.server.ProtocolHandler;
import spiralcraft.server.ProtocolHandlerFactory;

import spiralcraft.service.ServiceResolver;
import spiralcraft.service.ServiceException;

/**
 * A simple server which echoes input to output
 */
public class EchoServer
  extends Server
  implements ProtocolHandlerFactory
{
  
  public void init(ServiceResolver resolver)
    throws ServiceException
  { 
    setProtocolHandlerFactory(this);
    super.init(resolver);
  }

  public ProtocolHandler createProtocolHandler()
  { return new EchoProtocolHandler();
  }

  public void discardProtocolHandler(ProtocolHandler handler)
  {
  }

}
