package spiralcraft.net;

public interface ChannelListener
{
  void channelAccept(ChannelEvent event);
  
  void channelConnect(ChannelEvent event);
  
  void channelRead(ChannelEvent event);
  
  void channelWrite(ChannelEvent event);
  
  
}
