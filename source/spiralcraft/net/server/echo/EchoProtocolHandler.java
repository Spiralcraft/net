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
package spiralcraft.net.server.echo;


import spiralcraft.net.Connection;
import spiralcraft.net.server.ProtocolHandler;
import spiralcraft.net.server.ProtocolHandlerSupport;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class EchoProtocolHandler
  implements Runnable,ProtocolHandler
{
  
  private static int _ID=0;

  private ProtocolHandlerSupport _support;
  private Connection _connection;
  private InputStream _in;
  private OutputStream _out;
  private int _id=_ID++;

  public void handleConnection(ProtocolHandlerSupport support,Connection connection)
  {
    try
    {
      _support=support;
      _connection=connection;
      
      _in=connection.getInputStream();
      _out=connection.getOutputStream();
      _support.runBlockingOperation(this);
    }
    catch (IOException x)
    { 
      x.printStackTrace();
      try
      { connection.close();
      }
      catch (IOException x2)
      { x2.printStackTrace();
      }
      _support.protocolFinished(this);
    }
    
  }

  public void run()
  {
    try
    {
      byte[] buf=new byte[8192];
      boolean done=false;
      while (!done)
      {
        int len=_in.read(buf);
        if (len>-1)
        { 
          _out.write(buf,0,len);
          _out.flush();
        }
        else if (len==-1)
        { done=true;
        }
      }
      
    }
    catch (IOException x)
    { x.printStackTrace();
    }
    finally
    { 
      try
      { _connection.close();
      }
      catch (IOException x)
      { x.printStackTrace();
      }
      _support.protocolFinished(this);
    }
  }

  @Override
  public String toString()
  { return "spiralcraft.server.echo.EchoProtocolHandler-"+_id;
  }
}
