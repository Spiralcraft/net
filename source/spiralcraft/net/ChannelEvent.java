package spiralcraft.net;

import java.util.EventObject;

import java.nio.channels.SelectableChannel;

public class ChannelEvent
  extends EventObject
{
  
  public ChannelEvent(SelectableChannel channel)
  { super(channel);
  }
  
 
  public SelectableChannel getChannel()
  { return (SelectableChannel) getSource();
  }
}
