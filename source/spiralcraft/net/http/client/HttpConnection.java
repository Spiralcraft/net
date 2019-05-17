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
  private int readTimeout;
  private Request request;
  private Response response;
  private int outputBufferSize=1514;
  private int inputBufferSize=1514;
  private boolean lastRequest;
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
  
  public void setReadTimeout(int readTimeout)
  { this.readTimeout=readTimeout;
  }
  
  public void connect()
    throws IOException
  {
    if (socket!=null)
    { throw new IllegalStateException("Cannot be connected multiple times");
    }
    
    socket=getSocketFactory().createSocket();
    socket.setSoTimeout(readTimeout);
    socket.connect(new InetSocketAddress(address,port));
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
  
  public void readContent
    (OutputStream content
    ,InputStream in
    ,ContentLengthHeader clh
    )
    throws IOException
  {
    if (clh!=null)
    {
      
      StreamUtil.copyRaw
        (in
        ,content
        ,inputBufferSize
        ,clh.getLength()
        );
    }
    else
    {
      StreamUtil.copyRaw
        (in
        ,content
        ,inputBufferSize
        ,-1
        );
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
    response.start(inputStream);
    if (logLevel.isFine())
    { log.fine("Response started");
    }

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
        if (response.isChunkedTransferCoding())
        {
          readChunkedContent
            (contentOut
            ,inputStream
            );
        }
        else
        {
          readContent
            (contentOut
            ,inputStream
            ,(ContentLengthHeader) response.getHeader(ContentLengthHeader.NAME)
            );
        }
        contentOut.flush();
        contentOut.close();
          
      }

      if (!response.isKeepalive())
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
  
  private final void readChunkedContent(OutputStream out,InputStream in)
    throws IOException
  {
    int nextChunk=readChunkLen(in);
    do
    { 
      StreamUtil.copyRaw(in, out, 2048, nextChunk);
      readCRLF(in);
      nextChunk=readChunkLen(in);
    }
    while (nextChunk>0);
    
  }  
  
  private final void readCRLF(InputStream in)
    throws IOException
  {
    int c=in.read();
    if (c==-1)
    { throw new IOException("Unexpected EOF");
    }
    if (c==13)
    {
      if (in.read()!=10)
      { throw new IOException("Expected LF");
      }
      return;
    }
    
  }
  
  private final int readChunkLen(InputStream in)
    throws IOException
  {
    StringBuilder hexBuf=new StringBuilder();
    while (true)
    {
      int c=in.read();
      if (c==-1)
      { throw new IOException("Unexpected EOF");
      }
      if (c==13)
      {
        if (in.read()!=10)
        { throw new IOException("Expected LF");
        }
        int chunkLen=Integer.parseInt(hexBuf.toString(),16);
        return chunkLen;
      }
      hexBuf.append((char) c);
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
