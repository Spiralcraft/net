package spiralcraft.server;

import java.io.OutputStream;
import java.io.IOException;

public class ServerOutputStream
  extends OutputStream
{
  private final ServerConnection _connection;
  private final OutputStream _out;

  public ServerOutputStream(ServerConnection connection,OutputStream out)
  { 
    _connection=connection;
    _out=out;
  }

  public void write(int val)
    throws IOException
  { 
    _out.write(val);
    _connection.bytesWritten((byte) val);
  }
  
  public void write(byte[] bytes)
    throws IOException
  { 
    _out.write(bytes);
    _connection.bytesWritten(bytes);
  }

  public void write(byte[] bytes,int start,int len)
    throws IOException
  {
    _out.write(bytes,start,len);
    _connection.bytesWritten(bytes,start,len);;
  }

  public void flush()
    throws IOException
  { _out.flush();
  }

  public void close()
    throws IOException
  { _out.close();
  }
}
