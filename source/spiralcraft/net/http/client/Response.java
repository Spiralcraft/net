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
package spiralcraft.net.http.client;


import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.ConnectionHeader;
import spiralcraft.net.mime.ContentLengthHeader;
import spiralcraft.net.mime.MimeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.net.syntax.InetTextMessages;
import spiralcraft.net.syntax.SyntaxException;
import spiralcraft.util.ArrayUtil;
import spiralcraft.util.string.StringUtil;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.StreamUtil;

/**
 * Encapsulates an HTTP response at the client side
 */
public final class Response
  implements AutoCloseable
{
  private static final ClassLog log=ClassLog.getInstance(Response.class);
  private int status=0;
  private String reason;
  private String protocol;
  private final StringBuffer lineBuffer=new StringBuffer();
  private MimeHeaderMap headers=new MimeHeaderMap();
  private Resource content;
  private Level logLevel=Level.INFO;
  private ResponseStream clientStream;
  private Exchange exchange;
  
  /**
   * Read the entire response.
   */
  public void readResponse(InputStream in)
    throws IOException
  { 
    start(in);
  }

  public void setLogLevel(Level logLevel)
  { this.logLevel=logLevel;
  }
  
  public void start(InputStream in)
    throws IOException
  { 
    
    readStatus(in);
    for ( MimeHeader header=null; (header=readHeader(in))!=null; )
    { headers.add(header);
    }
    
  }

  
  /**
   * Whether the connection persists after this response (RFC 9112 9.3):
   *   not if a Connection header has the close option; otherwise yes for
   *   HTTP/1.1 and later, and for HTTP/1.0 only with keep-alive.
   */
  public boolean isKeepalive()
  {
    boolean keepAlive=false;
    List<MimeHeader> list=headers.getHeaders(ConnectionHeader.NAME);
    if (list!=null)
    {
      for (MimeHeader header: list)
      {
        ConnectionHeader hdr=(ConnectionHeader) header;
        if (hdr.isClose())
        { return false;
        }
        keepAlive=keepAlive || hdr.isKeepalive();
      }
    }
    return keepAlive || isHttp11OrLater();
  }

  /** Whether the status line names HTTP/1.1 or a later version */
  private boolean isHttp11OrLater()
  {
    if (protocol==null || !protocol.startsWith("HTTP/"))
    { return false;
    }
    String[] version=protocol.substring(5).split("\\.");
    try
    {
      int major=Integer.parseInt(version[0]);
      int minor=version.length>1?Integer.parseInt(version[1]):0;
      return major>1 || (major==1 && minor>=1);
    }
    catch (NumberFormatException x)
    { return false;
    }
  }
  
  public boolean isChunkedTransferCoding()
  {
        
    MimeHeader hdr=headers.getHeader("Transfer-Encoding");
    if (hdr!=null)
    { 
      String vs=hdr.getRawValue().toLowerCase();
      String[] va=vs.split(",");      
      return va.length>0 && va[va.length-1].trim().equals("chunked");
    }
    else
    { return false;
    }
  }
  
  public MimeHeader getHeader(String name)
  { return headers.getHeader(name);
  }
  
  public MimeHeaderMap getHeaders()
  { return headers;
  }
  
  void startStream(ResponseStream in)
  { this.clientStream=in;
  }

  void setExchange(Exchange exchange)
  { this.exchange=exchange;
  }
  
  public ResponseStream getInputStream()
  { 
    if (clientStream!=null)
    { return clientStream;
    }
    else
    { throw new IllegalStateException("Response is not streaming");
    }
  }
  
  public String getContentAsString()
    throws IOException
  {
    if (content!=null)
    { 
      return StreamUtil.readAsciiString
        (content.getInputStream(),(int) content.getSize()
        );
    }
    else
    { return null;
    }
  }
  
  /**
   * Whether content should be read from the inputStream of a partially completed
   *   request
   *  
   * @return
   */
  protected boolean shouldReadContent()
  {
    ContentLengthHeader hdr
      =(ContentLengthHeader) headers.getHeader(ContentLengthHeader.NAME);
    return hdr==null || hdr.getLength()!=0;
  }
  
  public long getContentLength()
  { 
    ContentLengthHeader hdr
      =(ContentLengthHeader) headers.getHeader(ContentLengthHeader.NAME);
    if (hdr!=null)
    { return hdr.getLength();
    }
    else
    { return -1;
    }
  }
  
  /**
   * The destination resource for any content
   * 
   * @param content
   */
  public void setContent(Resource content)
  { this.content=content;
  }
  
  /**
   * The destination resource for any content
   * 
   * @return
   */
  public Resource getContent()
  { return content;
  }
  
  private final void readStatus(InputStream in) 
    throws IOException
  { 
    String statusLine="";
    while (statusLine.isEmpty())
    { 
      statusLine=InetTextMessages.readHeaderLine(in,lineBuffer);
      if (statusLine.isEmpty())
      { throw new IOException("Premature end of input");
      }
    }
    if (logLevel.isFine())
    { log.fine(super.toString()+"Status line: "+statusLine);
    }
    String[] elements=statusLine.split(" ");
    if (elements.length<3)
    { throw new IOException(new SyntaxException("Bad status line ["+statusLine+"]"));
    }
    protocol=elements[0];
    status=Integer.parseInt(elements[1]);
    if (elements.length==3)
    { reason=elements[2];
    }
    else
    { reason=StringUtil.implode(' ',' ', ArrayUtil.tail(elements,2));
    }
    lineBuffer.setLength(0);
  }
  
 
  private final MimeHeader readHeader(InputStream in)
    throws IOException
  { 
    String headerLine=InetTextMessages.readHeaderLine(in,lineBuffer);
    if (headerLine==null)
    { throw new IOException("Premature end of input");
    }
    lineBuffer.setLength(0);
    if (headerLine.isEmpty())
    { return null;
    }
    else
    { return MimeHeader.parse(headerLine,"\\\"");
    }
  }
  


  public static String toString(byte[] bytes,int len)
  { 
    char[] chars=new char[len];
    for (int i=len-1;i>=0;i--)
    { chars[i]=(char) bytes[i];
    }
    return new String(chars);
  }


  
  public String getProtocol()
  { return protocol;
  }
  
  public int getStatus()
  { return status;
  }
  
  public String getReason()
  { return reason;
  }
  
  @Override
  public String toString()
  {
    StringBuffer out=new StringBuffer();
    out.append(protocol+" "+status+" "+reason);
    out.append("\r\n");
    for (List<MimeHeader> headerList: headers.values())
    {
      for (MimeHeader header: headerList)
      {
        out.append(header.toString());
        out.append("\r\n");
      }
    }
    out.append("\r\n");
    if (content!=null)
    { 
      try
      { out.append(getContentAsString());
      }
      catch (IOException x)
      { out.append("ERROR READING CONTENT"+x.toString());
      }
    }
    return out.toString();
  }

  /**
   * Close the Exchange this response came from, or the stream if there is
   *   none. Buffered content stays usable.
   */
  @Override
  public void close()
  { 
    if (exchange!=null)
    { exchange.close();
    }
    else
    { closeStream();
    }
  }

  void closeStream()
  {
    if (clientStream!=null)
    { clientStream.close();
    }
  }
  
  
}
