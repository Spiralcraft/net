package spiralcraft.server.echo;

import spiralcraft.server.ProtocolHandler;
import spiralcraft.server.ProtocolHandlerSupport;

import spiralcraft.net.Connection;

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

  public String toString()
  { return "spiralcraft.server.echo.EchoProtocolHandler-"+_id;
  }
}
