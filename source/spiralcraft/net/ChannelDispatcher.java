package spiralcraft.net;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import java.io.IOException;

/**
 * An interface that dispatches Channel events to interested parties,
 *   regardless of how the implementation decides to manage the associated
 *   Selector.
 */
public interface ChannelDispatcher
{

  /**
   * Register a ChannelListener to be notified of events which occur on
   *   the Selectable channel
   */
  SelectionKey registerChannel(SelectableChannel channel,ChannelListener listener)
    throws IOException;
  
  /**
   * Notify the dispatcher that event selections have changed and pending
   *   events need to be re-checked.
   */
  void wakeup();

}
