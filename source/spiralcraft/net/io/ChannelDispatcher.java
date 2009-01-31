//
// Copyright (c) 1998,2005 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net.io;

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
