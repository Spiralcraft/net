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
   * Signal the Endpoint that it should start accepting Connections
   *   and notifying listeners.
   */
  void bind()
    throws IOException;
  
  /**
   * Signal the Endpoint that it should stop accepting Connections
   */
  void release();
}
