package spiralcraft.net;

import java.io.IOException;

/**
 * A network communication endpoint which provides
 *   Connections.
 *
 * When an Endpoint 'connects', a ConnectionEvent is
 *   passed to registered listeners.
 */
public interface Endpoint
{

  void addConnectionListener(ConnectionListener listener);

  void removeConnectionListener(ConnectionListener listener);

  /**
   * Initialize the Endpoint- must be called before bind() 
   *   and supportsNonBlockingIO()
   */
  void init();
  
  /**
   * Signal the Endpoint that it should start making/accepting Connections
   *   and notifying listeners in blocking mode.
   *
   *@throws IOException if the Endpoint is already bound, or experiences
   *   some other problem obtaining IO resources.
   */
  void bind()
    throws IOException;
  
  /**
   * Signal the Endpoint that it should start making/accepting Connections
   *   and notifying listeners in non-blocking mode. The Endpoint will register
   *   with the provided dispatcher to receive channel events as they occur.
   *
   *@throws IOException if the Endpoint is already bound, or experiences
   *   some other problem obtaining IO resources.
   */
  void bind(ChannelDispatcher dispatcher)
    throws IOException;

  /**
   * Indicate whether the Endpoint supports non-blocking IO. Use cases which
   *   support non-blocking IO should query this method to determine how to
   *   handle bind() and subsequent IO for connections established through 
   *   this Endpoint.
   */
  boolean supportsNonBlockingIO();
  
  /**
   * Signal the Endpoint that it should stop making/accepting Connections
   */
  void release();
  
}
