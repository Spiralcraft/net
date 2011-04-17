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
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import spiralcraft.codec.text.Base64Codec;
import spiralcraft.log.Level;
import spiralcraft.log.Log;
import spiralcraft.log.ClassLog;

/**
 *
 *@author Michael Toth
 */
public class SMTPConnection
{
  public static final int DEFAULT_PORT=25;
  public static final int  DEFAULT_SSL_PORT=465;
  
  private static final ClassLog log
    =ClassLog.getInstance(SMTPConnection.class);
  
  public static void test(String tester, String server)
  {

    SMTPConnection con=new SMTPConnection();
    con.setServerName(server);
    con.setProtocolLog(log);
    
    if (con.connect())
    {
      System.out.println("Connected.");
      List<String> to=new ArrayList<String>();
      to.add(tester);
      if (con.sendMessage(tester,to,"TEST"))
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
  
  private String serverName = "pop";
  private int serverPort = 25;
  private String username;
  private String password;
  
  private Socket socket = null;
  private String errorStatus="Not Connected";
  private BufferedReader in = null;
  private BufferedWriter out = null;
  private String lastResponse="";

  private String connectResponse="";
  private String heloResponse="";

  private String localHost=null;
  private int soTimeout=0;
  private Log protocolLog;
  private Exception exception;
  private boolean needsReset;
  private boolean requireSSL;
  
  private SocketFactory socketFactory;
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONFIGURATION
  //
  ////////////////////////////////////////////////////////////////////////////

  public String getServerName()
  { return serverName;
  }

  public void setServerName(String n)
  { serverName=n;
  }

  public void setUsername(String username)
  { this.username=username;
  }
  
  public void setPassword(String password)
  { this.password=password;
  }
  
  public int getServerPort()
  { return serverPort;
  }

  public void setServerPort(int p)
  { serverPort=p;
  }

  
  public void setRequireSSL(boolean requireSSL)
  { this.requireSSL=requireSSL;
  }
  
  public void setSoTimeout(int timeoutMs)
  { soTimeout=timeoutMs;
  }

  public void setProtocolLog(Log log)
  { protocolLog=log;
  }


  ////////////////////////////////////////////////////////////////////////////
  //
  // MONITORING
  //
  ////////////////////////////////////////////////////////////////////////////

  public String getStatus()
  { return errorStatus;
  }

  public String getHeloResponse()
  { return heloResponse;
  }

  public String getConnectResponse()
  { return connectResponse;
  }

  public String getLastResponse()
  { return lastResponse;
  }

  public Exception getException()
  { return exception;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRODUCTION
  //
  ////////////////////////////////////////////////////////////////////////////
    


  public synchronized boolean connect()
  {
    if (socketFactory==null)
    { 
      if (requireSSL)
      { 
        if (serverPort==DEFAULT_PORT)
        { serverPort=DEFAULT_SSL_PORT;
        }
        SSLSocketFactory sslSocketFactory
          =(SSLSocketFactory) SSLSocketFactory.getDefault();
        
        socketFactory=sslSocketFactory;
      }
      else
      { socketFactory=SocketFactory.getDefault();
      }
    }
    needsReset=false;
    if (localHost==null)
    {
      try
      { localHost=InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException x)
      {
        errorStatus="Cannot determine local hostname.";
        return false;
      }
    }

    try
    {
      socket=socketFactory.createSocket(InetAddress.getByName(serverName),serverPort);
      socket.setTcpNoDelay(true);
      if (protocolLog!=null)
      { protocolLog.log(Level.FINE,"Connected to "+serverName+":"+serverPort);
      }
      if (soTimeout>=0)
      { socket.setSoTimeout(soTimeout);
      }

      in = new BufferedReader
              (new InputStreamReader
                (socket.getInputStream()
                )
              );

      out = new BufferedWriter
              (new OutputStreamWriter
                (socket.getOutputStream()
                )
              );

      String resp=readResponse();
      connectResponse=resp;
      
      if (resp==null || !resp.startsWith("220"))
      {
        errorStatus="Error on connect. Response to was: "+resp;
        return false;
      }
      while (resp.startsWith("220-"))
      { resp=readResponse();
      }

      if (username!=null)
      { 
        if (!login())
        { return false;
        }
      }
      else
      { 
        if (!sendHelo())
        { return false;
        }
      }
      
      return true;
    }
    catch (IOException x)
    { 
      exception=x;
      errorStatus="Caught IOException trying to connect to "
                      +serverName+" port "
                      +serverPort+": "+x.getMessage();
      if (protocolLog!=null)
      { protocolLog.log(Level.FINE,errorStatus);
      }
    }
    return false;
  }

  
  public boolean sendHelo()
    throws IOException
  {
    send("HELO "+localHost);
    heloResponse=readMultilineResponse()[0];
    if (!heloResponse.startsWith("250"))
    {
      errorStatus="Error on connect confirmation. Response to HELO was: "+heloResponse;
      return false;
    }

    errorStatus="";
    return true;
  }
  
  public boolean login()
    throws IOException
  {
    send("EHLO "+localHost);
    heloResponse=readMultilineResponse()[0];
    if (!heloResponse.startsWith("250"))
    {
      errorStatus
        ="Error on connect confirmation. Response to EHLO was: "+heloResponse;
      return false;
    }

    
    send("AUTH LOGIN");
    
    while (true)
    {
      String[] response=readMultilineResponse();
    
      if (response[0].startsWith("235"))
      { return true;
      }
      else if (response[0].startsWith("334"))
      {
        String challenge=Base64Codec.decodeAsciiString(response[0].substring(4));
        if (protocolLog!=null)
        { 
          protocolLog.log
            (Level.TRACE,"( Challenge: "+challenge+" )");
        }

        if (challenge.equals("Username:"))
        { send(Base64Codec.encodeAsciiString(username));
        }
        else if (challenge.equals("Password:"))
        { send(Base64Codec.encodeAsciiString(password));
        }

      }
      else
      {
        errorStatus
          ="Error negotiating SMTPAuth session: Response was "
            +Arrays.toString(response);
        return false;
      }
      
    }
  }
  
  public synchronized void disconnect()
  {
    if (protocolLog!=null)
    { protocolLog.log(Level.FINE,"<<< Disconnect");
    }
    try
    {
      if (socket!=null)
      {
        socket.close();
        errorStatus="Not Connected";
      }
    }
    catch (IOException x)
    { }
  }

  private synchronized String[] readMultilineResponse()
    throws IOException
  { 
    ArrayList<String> list=new ArrayList<String>();
    String line=readResponse();
    while (line.charAt(3)=='-')
    { 
      list.add(line);
      line=readResponse();
    }
    list.add(line);
    return list.toArray(new String[list.size()]);
  }
  
  private synchronized String readResponse()
    throws IOException
  {
    String line;
    try
    {
      line=in.readLine();
      if (line==null)
      { throw new IOException("Disconnected");
      }
      lastResponse=line;
      if (protocolLog!=null)
      { protocolLog.log(Level.FINE,">>> "+line);
      }
      return line;
    }
    catch (IOException x)
    { 
      errorStatus="Caught IOException getting response: "+x.getMessage();
      if (protocolLog!=null)
      { protocolLog.log(Level.FINE,">>> "+errorStatus);
      }
      exception=x;
      throw x;
    }
  }

  /**
   * Send an SMTP message.
   *@param sender The address of the sender (an Rfc 821 reverse-path, eg. "John Doe <johndoe@xyz.com>")
   *@param recipients A list of recipient addresses (an Rfc 821 forward path eg. "John Doe <johndoe@xyz.com>") 
   *@param message The message body
   */
  public synchronized boolean
    sendMessage(String sender,List<String> recipients,String message)
  { 
    if (socket==null || socket.isClosed())
    { throw new IllegalStateException("Not connected");
    }
    
    exception=null;
    try
    {
      if (needsReset)
      { 
        send("RSET");
        readResponse();
      }


      send("MAIL FROM:"+cleanAddress(sender));
      readResponse();
      if (!lastResponse.startsWith("250"))
      { 
        needsReset=true;
        return false;
      }
  
      for (String recipient: recipients)
      {
        send("RCPT TO:"+cleanAddress(recipient));
        readResponse();
        if (lastResponse.startsWith("250-"))
        {
          while (lastResponse.startsWith("250-"))
          { readResponse();
          }
        }
        if (!lastResponse.startsWith("250 "))
        { 
          needsReset=true;
          return false;
        }
      }
  
      send("DATA");
      readResponse();
      if (!lastResponse.startsWith("354"))
      { 
        needsReset=true;
        return false;
      }
  
      send(message);
      send(".");
      readResponse();
      if (!lastResponse.startsWith("250"))
      { 
        needsReset=true;
        return false;
      }
      needsReset=true;
  
      return true;
    }
    catch (IOException x)
    {
      errorStatus="Caught IOException sending message: "+x.getMessage();
      if (protocolLog!=null)
      { protocolLog.log(Level.FINE,">>> "+errorStatus);
      }
      exception=x;
      needsReset=true;
      return false;
    }
  }

  private String cleanAddress(String address)
  {
    int pos=address.indexOf('<');
    if (pos<0)
    { address="<".concat(address);
    }
    pos=address.indexOf('>');
    if (pos<0)
    { address=address.concat(">");
    }
    return address;
  }

  private synchronized void send(String msg)
    throws IOException
  {
    out.write(msg);
    out.write("\r\n");
    out.flush();
    if (protocolLog!=null)
    { protocolLog.log(Level.FINE,"<<< "+msg);
    }
  }
}
