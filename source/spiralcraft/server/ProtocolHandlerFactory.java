package spiralcraft.server;

/**
 * Creates and discards ProtocolHandlers
 */
public interface ProtocolHandlerFactory
{
  public ProtocolHandler createProtocolHandler();

  public void discardProtocolHandler(ProtocolHandler handler);

}
