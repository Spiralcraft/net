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
