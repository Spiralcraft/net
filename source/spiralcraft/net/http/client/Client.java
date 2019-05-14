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

import java.io.IOException;
import java.net.URI;

import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;
import spiralcraft.net.http.ConnectionHeader;
import spiralcraft.net.mime.ContentDispositionHeader;
import spiralcraft.net.mime.ContentLengthHeader;
import spiralcraft.net.mime.ContentTypeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.util.ByteArrayResource;



/**
 * A basic HTTP Client
 */
public class Client
{
  private static final ClassLog log
      =ClassLog.getInstance(Client.class);
  
  static
  {
    ContentLengthHeader.init();
    ConnectionHeader.init(); 
    ContentTypeHeader.init();
    ContentDispositionHeader.init();
  }

  private boolean debugStream;
  private Level logLevel=Level.INFO;
  
  public void setLogLevel(Level logLevel)
  { this.logLevel=logLevel;
  }
  
  public void setDebugStream(boolean debugStream)
  { this.debugStream=debugStream;
  }
  
  public Response get(URI uri)
    throws IOException
  { return executeRequest("GET",uri,null,null);
  }
  
  public Response executeRequest(URI uri)
      throws IOException
  { return executeRequest("GET",uri,null,null);
  }
  
  public Response executeRequest(String method,URI uri,String contentType,Resource content)
      throws IOException
  { return executeRequest(method,uri,contentType,content,null);
  }
   
  public Response executeRequest
    (String method
    ,URI uri
    ,String contentType
    ,Resource content
    ,MimeHeaderMap headers
    )
    throws IOException
  { 
    Request request=new Request();
    request.setMethod(method);
    if (uri.getPath()!=null && !uri.getPath().isEmpty())
    { request.setPath(uri.getPath());
    }
    if (uri.getQuery()!=null)
    { request.setQuery(uri.getQuery());
    }
    if (content!=null)
    { 
      request.setContent(content);
      request.setContentType(contentType);
      request.setContentLength(content.getSize());
    }
    if (headers!=null)
    { request.addHeaders(headers);
    }
    
    return executeRequest
      (uri.getScheme()
      ,uri.getHost()
      ,uri.getPort()>0
        ?uri.getPort()
        :uri.getScheme().equalsIgnoreCase("https")
        ?443
        :80
      ,request
      );
  }
    
  public Response executeRequest
    (String scheme,String hostName,int port,Request request)
    throws IOException
  {
    if (logLevel.isFine())
    { log.fine("Requesting: "+request);
    }
    
    HttpConnection connection=getConnection(scheme,hostName,port);
    
    connection.startRequest(request);
    connection.completeRequest();
    
    Response response=connection.startResponse();
    response.setContent(new ByteArrayResource());
    connection.completeResponse();
    if (logLevel.isFine())
    { log.fine("Got response: request: "+request+" response:"+response);
    }
    return response;
    
  }
  
  private HttpConnection getConnection(String scheme,String hostName,int port)
    throws IOException
  { 
    HttpConnection connection;
    if (scheme.equalsIgnoreCase("http"))
    { connection=new HttpConnection(hostName,port);
    }
    else if (scheme.equalsIgnoreCase("https"))
    { connection=new HttpsConnection(hostName,port);
    }
    else
    { throw new IOException("Unsupported protocol "+scheme);
    }
    if (debugStream)
    { 
      if (logLevel.isFine())
      { log.fine("Debug stream on");
      }
      connection.setDebugStream(true);
    }
    return connection;
  }
  
}