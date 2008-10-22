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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import spiralcraft.log.ClassLogger;
import spiralcraft.net.syntax.MailAddress;

public class SMTPConnector
{
  
  private static final Logger log
    =ClassLogger.getInstance(SMTPConnector.class);

  private String server;
  
  public void setServer(String server)
  { this.server=server;
  }
  
  
  public void send(Envelope envelope)
    throws IOException
  { 
    SMTPConnection con=new SMTPConnection();
    con.setServerName(envelope.getServer()!=null?envelope.getServer():server);
    // con.setProtocolLog(log);
    
    try
    {
      if (con.connect())
      {
        log.fine("Connected.");

        List<MailAddress> recipients=envelope.getRecipients();
        List<String> smtpList=new ArrayList<String>(recipients.size());
        for (MailAddress address : recipients)
        { smtpList.add(address.getSMTPPath());
        }

        if (con.sendMessage
            (envelope.getSenderMailAddress().getSMTPPath()
                ,smtpList
                ,envelope.getEncodedMessage()
            )
        )
        { 

        }
        else 
        { 
          log.fine("Failed to send message.");
          if (con.getException()!=null)
          { 
            if (con.getException() instanceof IOException)
            { throw (IOException) con.getException();
            }
            else
            { throw new IOException(con.getException());
            }
          }
        }
      }
      else
      { 
        log.fine(con.getStatus());
        if (con.getException()!=null)
        { 
          if (con.getException() instanceof IOException)
          { throw (IOException) con.getException();
          }
          else
          { throw new IOException(con.getException());
          }
        }
      }
    }
    finally
    {
      con.disconnect();
    }
    
  }
}