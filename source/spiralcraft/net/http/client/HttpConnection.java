//
// Copyright (c) 2012 Michael Toth
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
package spiralcraft.net.http.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import spiralcraft.io.DebugInputStream;
import spiralcraft.io.DebugOutputStream;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.log.Log;
import spiralcraft.net.mime.ContentLengthHeader;
import spiralcraft.vfs.StreamUtil;
import spiralcraft.vfs.util.ByteArrayResource;


/**
 * An HTTP connection
 */
public class HttpConnection
{
  private static final ClassLog log
    =ClassLog.getInstance(HttpConnection.class);
  private Level logLevel=Log.INFO;
  
  private String hostname;
  private int port;
  private InetAddress address;
  private Socket socket;
  private OutputStream outputStream;
  private InputStream inputStream;
  private int connectTimeoutMs;
  private int readTimeoutMs;
  private Request request;
  private Response response;
  private int outputBufferSize=1514;
  private boolean lastRequest;
  private boolean closeRequested;
  private boolean debugStream;
  int defaultPort=80;
  
  /**
   * Create a new connection to the specified host/port  
   * @param socket
   */
  public HttpConnection(String hostname,int port)
    throws UnknownHostException
  { 
    this.hostname=hostname;
    this.port=port;
    this.address=InetAddress.getByName(hostname);
  }

  /**
   * Create a new connection to the specified address/port  
   * @param socket
   */
  public HttpConnection(InetAddress address,int port)
  { 
    this.address=address;
    this.port=port;
  }

  public void setDebugStream(boolean debugStream)
  { this.debugStream=debugStream;
  }
  
  public void setLogLevel(Level logLevel)
  { this.logLevel=logLevel;
  }

  public void setConnectTimeoutMs(int connectTimeout)
  { this.connectTimeoutMs=connectTimeout;
  }
  
  public void setReadTimeoutMs(int readTimeout)
  { this.readTimeoutMs=readTimeout;
  }
  
  public void connect()
    throws IOException
  {
    if (socket!=null)
    { throw new IllegalStateException("Cannot be connected multiple times");
    }
    
    socket=getSocketFactory().createSocket();
    socket.setSoTimeout(readTimeoutMs);
    socket.setReuseAddress(true);
    socket.connect(new InetSocketAddress(address,port),connectTimeoutMs);
    if (logLevel.isFine())
    { log.fine("Connected to "+address+":"+port);
    }
    outputStream=socket.getOutputStream();
    inputStream=socket.getInputStream();
    if (debugStream)
    {
      outputStream=new DebugOutputStream(outputStream);
      inputStream=new DebugInputStream(inputStream);
    }
    inputStream=new BufferedInputStream(inputStream,1514);
  }
  
  public void ensureConnected()
    throws IOException
  {
    if (socket==null)
    { connect();
    }
  }
  
  public void startRequest(Request request)
    throws IOException
  {
    if (lastRequest)
    { 
      throw new IllegalStateException
        ("No more requests can be sent on this connection");
    }
    ensureConnected();
    this.request=request;
    if (request.getHost()==null && hostname!=null)
    { request.setHost(hostname+(port!=defaultPort?":"+port:""));
    }
    closeRequested="close".equalsIgnoreCase(request.getHeaderValue("Connection"));
    request.start(outputStream);
    if (logLevel.isFine())
    { log.fine("Request committed");
    }
    ContentLengthHeader clh
      =(ContentLengthHeader) request.getHeader(ContentLengthHeader.NAME);
    
    if (request.getContent()!=null)
    {
      InputStream contentIn=request.getContent().getInputStream();
      try
      { sendContent(contentIn,clh);
      }
      finally
      { contentIn.close();
      }
    }
    
  }

  public void sendContent(InputStream content,ContentLengthHeader clh)
    throws IOException
  {
    if (clh!=null)
    {
      
      long len=StreamUtil.copyRaw
        (content
        ,outputStream
        ,outputBufferSize
        ,clh.getLength()
        );

        if (logLevel.isFine())
        { log.fine("Sent "+len+" bytes of content");
        }
    }
    else
    {
      // A request with content and no content length header 
      long len=StreamUtil.copyRaw
        (content
        ,outputStream
        ,outputBufferSize
        ,-1
        );
        if (logLevel.isFine())
        { log.fine("Sent "+len+" bytes of content, closing connection");
        }
      lastRequest=true;
    }
  }
   
  
  public void completeRequest()
    throws IOException
  { 
    if (request!=null)
    { 
      outputStream.flush();
      request=null;
      if (logLevel.isFine())
      { log.fine("Request completed");
      }
    }
    else
    { throw new IllegalStateException("Current request already completed");
    }
  }
  
  public Response startResponse()
    throws IOException
  {
    if (inputStream==null)
    { throw new IllegalStateException("Not connected");
    }
    response=new Response();
    response.setLogLevel(logLevel);
    response.start(inputStream);
    if (logLevel.isFine())
    { log.fine("Response started");
    }

    return response;
    
  }
  
  public Response startStreamingResponse()
    throws IOException
  {
    
    Response response = startResponse();
    response.startStream(new ResponseStream(inputStream,this,response,false));
    return response;
  }
  
  public void completeResponse()
    throws IOException
  {
    if (response!=null)
    { 
      if (response.shouldReadContent())
      {
        if (response.getContent()==null)
        { response.setContent(new ByteArrayResource());
        }
        
        OutputStream contentOut=response.getContent().getOutputStream();
        InputStream clientStream=new ResponseStream(inputStream,this,response,false);
        StreamUtil.copyRaw(clientStream, contentOut, 2048, -1);
        contentOut.flush();
        contentOut.close();          
      }
      
      if (!response.isKeepalive() 
          || closeRequested
         )
      { lastRequest=true;
      }
      response=null;
      if (lastRequest)
      { close();
      }
      if (logLevel.isFine())
      { log.fine("Response completed");
      }
    }
    else
    { throw new IllegalStateException("Current response already completed");
    }
    
  }
  

  public void close()
  {
    if (socket!=null)
    {
      try
      { socket.close();
      }
      catch (IOException x)
      {
      }
    }
  }
    
  protected SocketFactory getSocketFactory()
  { return SocketFactory.getDefault();
  }
}
