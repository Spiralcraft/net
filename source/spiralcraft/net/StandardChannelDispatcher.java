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
package spiralcraft.net;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.CancelledKeyException;

import java.util.Iterator;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import spiralcraft.registry.Registrant;
import spiralcraft.registry.RegistryNode;

import java.io.IOException;

/**
 * Translates ops on SelectableChannels into ChannelEvents
 */
public class StandardChannelDispatcher
  implements ChannelDispatcher,Runnable,Registrant
{
  
  private final Selector _selector;

  private Logger _logger;
  private Thread _thread;
  private boolean _finished;

  public StandardChannelDispatcher()
    throws IOException
  { _selector=Selector.open();
  }
  
  /**
   * Registrant.register(RegistryNode)
   *
   * The registry is used to find a Logger instance.
   */
  public void register(RegistryNode node)
  { _logger=(Logger) node.findInstance(Logger.class);
  }
    
  public void wakeup()
  { _selector.wakeup();
  }
  
  public void run()
  {
    try
    {
      while (!_finished) 
      {
        int numKeys=0;

        try
        { numKeys=_selector.select();
        }
        catch (NullPointerException x)
        { 
          if (_logger.isLoggable(Level.FINE))
          { _logger.fine("NPE in selector");
          }
          continue;
        }
        
        if (numKeys==0)
        { continue;
        }

        Set readyKeys = _selector.selectedKeys();
        Iterator it = readyKeys.iterator();
  
        while (it.hasNext()) 
        {
          SelectionKey key = (SelectionKey) it.next();
          it.remove();
  
          if (!key.isValid())
          { continue;
          }
          
          int ops=key.readyOps();
          ChannelListener listener=(ChannelListener) key.attachment();
          ChannelEvent event=new ChannelEvent(key.channel());
          
          try
          {
            if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
            { 
              logFine("Accept",listener);
              listener.channelAccept(event);
            }
            
            if ((ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT)
            { 
              logFine("Connect",listener);
              listener.channelConnect(event);
            }
            
            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ)
            { 
              logFine("Read",listener);
              listener.channelRead(event);
            }
            
            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
            { 
              logFine("Write",listener);
              listener.channelWrite(event);
            }
          }
          catch (CancelledKeyException x)
          { 
            if (_logger.isLoggable(Level.FINE))
            { _logger.fine("Cancelled key "+key.channel());
            }
          }
            
        }
      }
    }
    catch (IOException x)
    { x.printStackTrace();
    }
  }

  private void logFine(String message,ChannelListener listener)
  {
    if (_logger!=null && _logger.isLoggable(Level.FINE))
    { _logger.fine(message+": "+listener.toString());
    }
  }
  
  public synchronized void start()
  {
    _finished=false;
    if (_thread==null)
    {
      _thread=new Thread(this);
      _thread.setPriority(Thread.MAX_PRIORITY);
      _thread.setDaemon(true);
      _thread.start();
    }
  }
  
  public synchronized void stop()
  { 
    _finished=true;
    _selector.wakeup();
    _thread=null;
  }
  
  public SelectionKey registerChannel(SelectableChannel channel,ChannelListener listener)
    throws IOException
  { return channel.register(_selector,channel.validOps(),listener);
  }

}
