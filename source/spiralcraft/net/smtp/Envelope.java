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

import java.util.List;

public class Envelope
{

  private String server;
  private String sender;
  private List<String> recipients;
  private String message;
  
  /**
   * @return the sender
   */
  public String getSender()
  {
    return sender;
  }

  /**
   * @param sender the sender to set
   */
  public void setSender(
    String sender)
  {
    this.sender = sender;
  }

  /**
   * @return the recipients
   */
  public List<String> getRecipients()
  {
    return recipients;
  }

  /**
   * @param recipients the recipients to set
   */
  public void setRecipients(
    List<String> recipients)
  {
    this.recipients = recipients;
  }

  /**
   * @return the message
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(
    String message)
  {
    this.message = message;
  }

  /**
   * @return the server
   */
  public String getServer()
  {
    return server;
  }

  public void setServer(String server)
  { this.server=server;
  }
  
  
}
