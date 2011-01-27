//
// Copyright (c) 1998,2008 Michael Toth
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
package spiralcraft.net.smtp;

import java.util.LinkedList;
import java.util.List;


import spiralcraft.net.syntax.MailAddress;

import spiralcraft.text.ParseException;

public class Envelope
{
      
  private String server;
  private MailAddress sender;
  private List<MailAddress> recipients;
  private String encodedMessage;
  
  
  /**
   * @return the sender
   */
  public MailAddress getSenderMailAddress()
  { return sender;
  }

  /**
   * @param sender the sender to set
   */
  public void setSender(String sender)
  {
    if (sender==null)
    { this.sender=null;
    }
    else
    {
      try
      { this.sender = new MailAddress(sender);
      }
      catch (ParseException x)
      { throw new IllegalArgumentException(x);
      }
    }
    
  }

  /**
   * @return the recipients
   */
  public List<MailAddress> getRecipients()
  {
    return recipients;
  }

  /**
   * @param recipients the recipients to set
   */
  public void setRecipients(
    List<MailAddress> recipients)
  {
    this.recipients = recipients;
  }

  /**
   * @param Specify a single recipient
   */
  public void setRecipient(String recipient)
  { 
    this.recipients=new LinkedList<MailAddress>();
    if (recipient!=null)
    {
      try
      { this.recipients.add(new MailAddress(recipient));
      }
      catch (ParseException x)
      { throw new IllegalArgumentException(x);
      }
    }
  }
  
  /**
   * @return the message
   */
  public String getEncodedMessage()
  {
    return encodedMessage;
  }

  /**
   * @param message the message to set
   */
  public void setEncodedMessage(String message)
  { this.encodedMessage = message;
  }
  
  public void insertHeaders(HeaderBinding<?>[] headerBindings,boolean last)
  { 
    StringBuffer out=new StringBuffer();
    for (HeaderBinding<?> binding : headerBindings)
    { 
      out.append(binding.getName())
        .append(": ")
        .append(binding.get())
        .append("\r\n");
    }
    if (last)
    { out.append("\r\n");
    }
    if (this.encodedMessage!=null)
    { out.append(this.encodedMessage);
    }
    this.encodedMessage=out.toString();
  }

  /**
   * @return the server
   */
  public String getServer()
  { return server;
  }

  public void setServer(String server)
  { this.server=server;
  }
  
  @Override
  public String toString()
  {
     return super.toString()
       +"[  \r\n  server=["+server+"] " 
       +"\r\n  sender=["+sender+"] "
       +"\r\n  recipients=["+recipients+"] "
       +"\r\n  message=["+encodedMessage+"] \r\n";
  }
  
}
