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

import java.io.OutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import spiralcraft.net.http.Headers;
import spiralcraft.net.mime.ContentLengthHeader;
import spiralcraft.net.mime.ContentTypeHeader;
import spiralcraft.net.mime.GenericHeader;
import spiralcraft.net.mime.MimeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.vfs.Resource;


/**
 * Encapsulates an HTTP request at the client side
 */
public class Request
{
  private static final Charset ASCII=Charset.forName("US-ASCII");
	
  private String method="GET";
  private String path="/";
  private String query;
  private String protocol="HTTP/1.1";
  private MimeHeaderMap headers
    =new MimeHeaderMap();
  private Resource content;
  
  public Request()
  {
  }
  
  public void setMethod(String method)
  { this.method=method;
  }
  
  public void setHost(String host)
  { headers.put(new GenericHeader(Headers.HOST,host));
  }
  
  public void setPath(String path)
  { this.path=path;
  }

  public void setQuery(String query)
  { this.query=query;
  }
 
  
  public String getHost()
  { 
    MimeHeader header=headers.getHeader(Headers.HOST);
    return header!=null?header.getRawValue():null;
  }
  
  public void setAuthorization(String authorization)
  { 
    headers.put
      (new GenericHeader(Headers.AUTHORIZATION,authorization)
      );
  }

  public void setContent(Resource content)
  { this.content=content;
  }
  
  public Resource getContent()
  { return content;
  }
  
  public void setContentLength(long contentLength)
  { headers.put(new ContentLengthHeader(contentLength));
  }

  public void setContentType(String contentType)
    throws IOException
  { headers.put(new ContentTypeHeader(contentType));
  }
  
  public MimeHeader getHeader(String name)
  { return headers.getHeader(name);
  }
  
  boolean hasHeader(String name)
  { return headers.containsHeader(name);
  }
  
  public void addHeader(MimeHeader header)
  { headers.add(header);
  }
  
  public void addHeaders(MimeHeaderMap headers)
  { 
    ArrayList<MimeHeader> headerList = new ArrayList<>();
    headers.toValueList(headerList);
    for (MimeHeader header:headerList)
    { this.headers.add(header);
    }
  }
  
  public String toString()
  { return super.toString()+": "+requestStartBlock();
  }
  
  public final void start(OutputStream out)
      throws IOException
  { out.write(requestStartBlock().getBytes(ASCII));
  }

  final void writeRequestLine(StringWriter writer)
  {
    writer.write(method);
    writer.write(" ");
    writer.write(path);
    if (query!=null)
    {
      writer.write("?");
      writer.write(query);
    }
    writer.write(" ");
    writer.write(protocol);
  }

  final void writeHeaders(StringWriter writer)
  {
    for (List <MimeHeader> headerList : headers.values())
    {
      for (MimeHeader header : headerList)
      { 
        writer.write(header.getName());
        writer.write(": ");
        writer.write(header.getRawValue());
        writer.write("\r\n");
      }
    }
  }
  
  final String requestStartBlock()
  { 
    StringWriter writer=new StringWriter();
    writeRequestLine(writer);
    writer.write("\r\n");
    writeHeaders(writer);
    writer.write("\r\n");
    return writer.toString();
  }

  
  
}
