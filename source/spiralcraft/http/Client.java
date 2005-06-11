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
package spiralcraft.http;

import spiralcraft.exec.Executable;
import spiralcraft.exec.ExecutionContext;

import spiralcraft.stream.Resolver;

import java.net.URI;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;


/**
 * A basic HTTP Client
 */
public class Client
  implements Executable
{

  private String host;
  private int port=80;
  private Socket socket;
  private InputStream in;
  private OutputStream out;
  
  public void setHost(String host)
  { this.host=host;
  }
  
  public void setPort(int port)
  { this.port=port;
  }
  
  public synchronized void connect()
    throws IOException
  { 
    InetAddress addr=InetAddress.getByName(host);
    if (addr==null)
    { throw new IOException("Host "+host+" not found");
    }
    socket=new Socket(addr,port);
    in=socket.getInputStream();
    out=socket.getOutputStream();
  }
  
  public synchronized void disconnect()
    throws IOException
  { 
    try
    {
      in.close();
      out.close();
      socket.close();
    }
    finally
    { socket=null;
    }
    
  }

  public synchronized ClientResponse executeRequest(ClientRequest request)
    throws IOException
  {
    boolean autoConnected=(socket==null);
    if (socket==null)
    { connect();
    }
    request.write(out);
    ClientResponse response=new ClientResponse();
    response.readResponse(in);
    if (autoConnected)
    { disconnect();
    }
    return response;
  }
  
	public void execute(ExecutionContext context,String[] args)
	{ 
 	  ClientRequest req=new ClientRequest();
    OutputStream outputStream=null;
    
    String host="127.0.0.1";
    int port=80;
    String url="/";

		try
		{
			
      int i=0;
      for (i=0;i<args.length;i++)
      {
        if (args[i].startsWith("-"))
        { 
          String option=args[i].substring(1).intern();
          if (option=="content")
          { 
            req.setContentURI(URI.create(args[++i]));
          }
          else if (option=="host")
          { host=args[++i];
          }
          else if (option=="port")
          { port=Integer.parseInt(args[++i]);
          }
          else if (option=="path")
          { url=args[++i];
          }
          else if (option=="output")
          { 
            outputStream
              =Resolver.getInstance().resolve(URI.create(args[++i]))
                .getOutputStream();
          }
          else if (option=="authorization")
          { req.setAuthorization(args[++i]);
          }
          else
          { 
            throw new IllegalArgumentException
              ("Unrecognized option '"+option+"'");
          }
        }
      }
      req.setPath(url);
      req.setHost(host);
      
      setHost(host);
      setPort(port);

      ClientResponse response
        =executeRequest(req);

      if (outputStream!=null)
      { 
        outputStream.write(response.getRawData());
        outputStream.flush();
        outputStream.close();
      }
      else
      {
        
        context.out().println("Response [");
        context.out().println(new String(response.getRawData()));
        context.out().println("] End response");
      }

    }
		catch (Exception x)
		{ context.err().println(x.toString());
		}

  }
  
}