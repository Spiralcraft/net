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
