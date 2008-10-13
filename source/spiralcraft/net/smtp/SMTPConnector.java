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

public class SMTPConnector
{

  private String server;
  
  public void setServer(String server)
  { this.server=server;
  }
  
  
  public void send(Envelope envelope)
  { 
    SMTPConnection con=new SMTPConnection();
    con.setServerName(envelope.getServer()!=null?envelope.getServer():server);
    // con.setProtocolLog(log);
    
    if (con.connect())
    {
      System.out.println("Connected.");

      if (con.sendMessage
          (envelope.getSender()
          ,envelope.getRecipients()
          ,envelope.getMessage()
          )
         )
      { System.out.println("Message sent.");
      }
      else 
      { System.out.println("Failed to send message.");
      }
    }
    else
    { System.out.println(con.getStatus());
    }
    con.disconnect();
    
  }
}
