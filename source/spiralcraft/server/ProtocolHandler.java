package spiralcraft.server;

import spiralcraft.net.Connection;

/**
 * Runs a server protocol for a single connection
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
