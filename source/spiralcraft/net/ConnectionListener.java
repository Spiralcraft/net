package spiralcraft.net;

public interface ConnectionListener
{

  public void connectionEstablished(ConnectionEvent event);

  public void connectionClosed(ConnectionEvent event);
  
  
}
