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
import java.util.Arrays;
import java.util.List;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.syntax.MailAddress;

public class SMTPClient
  implements SMTPConnector
{
  
  private static final ClassLog log
    =ClassLog.getInstance(SMTPClient.class);

  private String server;
  private int port=25;
  private String username;
  private String password;
  private boolean debug;
  
  private boolean testMode;
  private List<MailAddress> testRecipients;
  private boolean requireSSL;
  
  /**
   * The SMTP server which will forward outgoing mail
   * 
   * @param server
   */
  public void setServer(String server)
  { this.server=server;
  }
  
  /**
   * The server port to connect to. Defaults to port 25, the standard
   *   SMTP port.
   */
  public void setPort(int port)
  { this.port=port;
  }
  
  public void setUsername(String username)
  { this.username=username;
  }
  
  public void setPassword(String password)
  { this.password=password;
  }
  
  /**
   * Require the use of SSL
   * 
   * @param requireSSL
   */
  public void setRequireSSL(boolean requireSSL)
  { this.requireSSL=requireSSL;
  }
  
  /**
   * Send the message to the log and the test recipients, if any, INSTEAD OF
   *   sending it to the envelope recipients. Used for development
   *   purposes.  
   * 
   * @param testMode
   */
  public void setTestMode(boolean testMode)
  { this.testMode=testMode;
  }  
  
  public void setTestRecipients(MailAddress[] recipients)
  { 
    this.testRecipients=new ArrayList<MailAddress>();
    this.testRecipients.addAll(Arrays.asList(recipients));
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  @Override
  public void send(Envelope envelope)
    throws IOException
  { 
    if (envelope==null)
    { throw new IllegalArgumentException("Envelope is null");
    }
    if (testMode && (testRecipients==null || testRecipients.isEmpty()))
    { 
      testSend(envelope);
      return;
    }
    if (!testMode && 
          (envelope.getRecipientList()==null 
          || envelope.getRecipientList().isEmpty()
          )
       )
    { 
      log.warning("No recipients in message from "+envelope.getSender());
      return;
    }
    SMTPConnection con=new SMTPConnection();
    if (requireSSL)
    { con.setRequireSSL(requireSSL);
    }
    con.setServerName(envelope.getServer()!=null?envelope.getServer():server);
    con.setServerPort(port);
    if (debug)
    { con.setProtocolLog(log);
    }
    if (username!=null)
    { con.setUsername(username);
    }
    if (password!=null)
    { con.setPassword(password);
    }
    
    try
    {
      if (con.connect())
      {
        log.fine("Connected.");

        List<MailAddress> recipients=envelope.getRecipientList();
        if (testMode && testRecipients!=null)
        { recipients=testRecipients;
        }
        
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
          log.fine("Failed to send message. Last response: "
            +con.getLastResponse()
            );
          if (con.getException()!=null)
          { 
            log.log(Level.WARNING,"Exception sending mail",con.getException());
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
  
  public void testSend(Envelope envelope)
  {
    log.warning
      ("SMTPConnector is in test mode. MESSAGE WILL NOT BE SENT"
      );
    log.info(envelope.toString());
    log.warning
      ("SMTPConnector is in test mode. MESSAGE NOT SENT"
      );
    
  }
  
}
