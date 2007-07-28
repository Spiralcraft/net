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
package spiralcraft.net.server;

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
