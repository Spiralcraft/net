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

//import spiralcraft.log.ClassLog;
import spiralcraft.net.http.ConnectionHeader;
import spiralcraft.net.mime.ContentDispositionHeader;
import spiralcraft.net.mime.ContentLengthHeader;
import spiralcraft.net.mime.ContentTypeHeader;
import spiralcraft.vfs.util.ByteArrayResource;



/**
 * A basic HTTP Client
 */
public class Client
{
//  private static final ClassLog log
//    =ClassLog.getInstance(Client.class);
  
  static
  {
    ContentLengthHeader.init();
    ConnectionHeader.init(); 
    ContentTypeHeader.init();
    ContentDispositionHeader.init();
  }

  private boolean debugStream;
  
  public void setDebugStream(boolean debugStream)
  { this.debugStream=debugStream;
  }
  
  public Response executeRequest(URI uri)
    throws IOException
  { 
    Request request=new Request();
    if (uri.getPath()!=null && !uri.getPath().isEmpty())
    { request.setPath(uri.getPath());
    }
    if (uri.getQuery()!=null)
    { request.setQuery(uri.getQuery());
    }
    
    return executeRequest
      (uri.getHost()
      ,uri.getPort()>0?uri.getPort():80
      ,request
      );
  }
    
  public Response executeRequest
    (String hostName,int port,Request request)
    throws IOException
  {
    
    HttpConnection connection=getConnection(hostName,port);
    
    connection.startRequest(request);
    connection.completeRequest();
    
    Response response=connection.startResponse();
    response.setContent(new ByteArrayResource());
    connection.completeResponse();
    return response;
    
  }
  
  private HttpConnection getConnection(String hostName,int port)
    throws IOException
  { 
    HttpConnection connection=new HttpConnection(hostName,port);
    if (debugStream)
    { connection.setDebugStream(true);
    }
    return connection;
  }
  
}