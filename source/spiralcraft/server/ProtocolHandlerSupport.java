package spiralcraft.server;

/**
 * Provides support for ProtocolHandlers
 */
public interface ProtocolHandlerSupport
{

  /**
   * Indicate that the protocol has been completed
   *   and that the ProtocolHandler can be re-used for
   *   a new Connection.
   */
  void protocolFinished(ProtocolHandler handler);

  /**
   * Run an potentially blocking operation in its own Thread.
   */
  void runBlockingOperation(Runnable runnable);
}
