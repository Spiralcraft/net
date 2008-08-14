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

import spiralcraft.net.Connection;
import spiralcraft.net.ConnectionListener;

import spiralcraft.util.StringUtil;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URI;

import java.text.MessageFormat;

public class ServerConnection
  implements Connection
{
  private static final byte[] EOL
    =StringUtil.asciiBytes(System.getProperty("line.separator"));

  private final Server _server;
  private final Connection _connection;
  private InputStream _inputStream;
  private OutputStream _outputStream;
  private final OutputStream _trace;

  private final MessageFormat _traceStreamSeparator
    = new MessageFormat("---[{0}:{1}]---");
  private final Object[] _traceFormatParameters=new Object[2];

  public ServerConnection(Server server,Connection connection)
  { 
    _server=server;
    _connection=connection;
    _trace=server.createTraceStream();
  }

  public void addConnectionListener(ConnectionListener listener)
  { _connection.addConnectionListener(listener);
  }

  public void removeConnectionListener(ConnectionListener listener)
  { _connection.removeConnectionListener(listener);
  }

  public InputStream getInputStream()
    throws IOException
  { 
    if (_inputStream==null)
    { _inputStream=new ServerInputStream(this,_connection.getInputStream());
    }
    return _inputStream;
  }

  public OutputStream getOutputStream()
    throws IOException
  { 
    if (_outputStream==null)
    { _outputStream=new ServerOutputStream(this,_connection.getOutputStream());
    }
    return _outputStream;
  }

  public void close()
    throws IOException
  { 
    _connection.close();
    _server.traceStreamFinished(_trace);
  }

  public void setReadTimeoutMillis(int millis)
    throws IOException
  { _connection.setReadTimeoutMillis(millis);
  }

  public boolean isSecure()
  { return _connection.isSecure();
  }

  public URI getRemoteAddress()
  { return _connection.getRemoteAddress();
  }

  public URI getLocalAddress()
  { return _connection.getLocalAddress();
  }

  @Override
  public String toString()
  { return _connection.toString();
  }

  /**
   * Notified when data is written
   */
  void bytesWritten(final byte val)
  { writeTrace('O',new byte[] {val},0,1);
  }

  /**
   * Notified when data is written
   */
  void bytesWritten(byte[] buffer)
  { transferred('O',buffer,0,buffer.length);
  }

  /**
   * Notified when data is written
   */
  void bytesWritten(byte[] buffer,int start,int len)
  { transferred('O',buffer,start,len);
  }

  /**
   * Notified when data is read
   */
  void bytesRead(final byte val)
  { transferred('I',new byte[] {val},0,1);
  }

  /**
   * Notified when data is read
   */
  void bytesRead(byte[] buffer,int start,int len)
  { transferred('I',buffer,start,len);
  }

  /**
   * Notified when data is read
   */
  void skipped(final long count)
  { _server.countBytesRead(count);
  }

  void transferred(char code,byte[] buffer,int start,int len)
  {
    if (_trace!=null)
    { writeTrace(code,buffer,start,len);
    }
    _server.countBytesRead(len);
  }
  
  synchronized void writeTrace(char code,byte[] buffer,int start,int len)
  {
    _traceFormatParameters[0]=new Character(code);
    _traceFormatParameters[1]=new Integer(len);
    try
    {
      _trace.write(EOL);
      _trace.write
        (StringUtil.asciiBytes
          (_traceStreamSeparator.format
            (_traceFormatParameters
            )
          )
        );
      _trace.write(EOL);
      _trace.write(buffer,start,len);
      _trace.write(EOL);
      _trace.write
        (StringUtil.asciiBytes
          (_traceStreamSeparator.format
            (_traceFormatParameters
            )
          )
        );
      _trace.write(EOL);
    }
    catch (IOException x)
    { x.printStackTrace();
    }
  }

}
