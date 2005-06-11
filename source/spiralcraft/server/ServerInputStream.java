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
package spiralcraft.server;

import java.io.InputStream;
import java.io.IOException;

public class ServerInputStream
  extends InputStream
{
  private final ServerConnection _connection;
  private final InputStream _in;

  public ServerInputStream(ServerConnection connection,InputStream in)
  { 
    _connection=connection;
    _in=in;

  }

  public final int available()
    throws IOException
  { return _in.available();
  }

  public final void close()
    throws IOException
  { _in.close();
  }

  public final void mark(int readlimit)
  { _in.mark(readlimit);
  }

  public final boolean markSupported()
  { return _in.markSupported();
  }

  public final int read()
    throws IOException
  {
    final int val=_in.read();
    if (val!=-1)
    { _connection.bytesRead((byte) val);
    }
    return val;
  }

  public final int read(byte[] buffer)
    throws IOException
  { 
    final int count=_in.read(buffer);
    if (count!=-1)
    { _connection.bytesRead(buffer,0,count);
    }
    return count;
  }

  public final int read(byte[] buffer,int start,int len)
    throws IOException
  { 
    final int count=_in.read(buffer,start,len);
    if (count!=-1)
    { _connection.bytesRead(buffer,start,count);
    }
    return count;
  }

  public final void reset()
    throws IOException
  { _in.reset();
  }  

  public final long skip(long n)
    throws IOException
  {
    final long count=_in.skip(n);
    if (count!=-1)
    { _connection.skipped(count);
    }
    return count;
  }

}
