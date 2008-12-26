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
package spiralcraft.net.http;


import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;

import java.util.HashMap;

import spiralcraft.util.ByteBuffer;

import spiralcraft.vfs.StreamUtil;

/**
 * Encapsulates an HTTP response at the client side
 */
public final class ClientResponse
{
	private ByteArrayOutputStream _rawData;
  private ByteBuffer _buf=new ByteBuffer();
	// private boolean _parsed=false;
	private List<Cookie> _cookies;
	private int _resultCode=0;
	private String _resultMessage;
	private String _protocol;
	private int _readPos=0;
  private InputStream _in;
  private HashMap<String,String> _headerMap=new HashMap<String,String>();
  private LinkedList<String> _headerList=new LinkedList<String>();
  private boolean _parseAllHeaders;
  private byte[] _content;
	
	private static final byte[] _cookieHeader="Set-Cookie:".getBytes();
	
  /**
   * Read the entire response.
   */
	public void readResponse(InputStream in)
		throws IOException
	{ 
    start(in);
    ByteArrayOutputStream out
      =new ByteArrayOutputStream();
    readContent(out);
    _content=out.toByteArray();
    
	}
	

  public void setParseAllHeaders(boolean val)
  { _parseAllHeaders=val;
  }

	public byte[] getRawData()
	{ return _rawData.toByteArray(); 
	}
	
  public List<String> getHeaderNames()
  { return _headerList;
  }

  public String getHeader(String name)
  { return _headerMap.get(name);
  }

  public static String toString(byte[] bytes,int len)
  { 
    char[] chars=new char[len];
    for (int i=len-1;i>=0;i--)
    { chars[i]=(char) bytes[i];
    }
    return new String(chars);
  }

	public int getByteCount()
	{
    // XXX This needs to accomodate data reading stream
    if (_rawData!=null)
    { return _rawData.size();
    }
    else
    { return 0;
    }
	}
	
	public String getProtocol()
	{ return _protocol;
	}
	
	public int getResultCode()
	{ return _resultCode;
	}
	
	public String getResultMessage()
	{ return _resultMessage;
	}
	
	public byte[] getContent()
  { return _content;
  }
  
  public void start(InputStream in)
    throws IOException
  { 
    _rawData=new ByteArrayOutputStream(8192);
    _in=new BufferedInputStream(in);
    readHeaders();
  }

  /**
   * Special mode to relay all data to an output stream.
   * Experimental for now.
   */
  @SuppressWarnings("unused") // XXX Use or delete
  private void pump(OutputStream out)
    throws IOException
  { 
    byte[] buf=new byte[8192];
    while (true)
    {
      if (_in.available()==0)
      { 
        out.flush();
        int single=_in.read();
        if (single<0)
        { break;
        }
        out.write(single);
      }
      else
      { 
        int readSize=Math.min(_in.available(),buf.length);
        // int count=_in.read(buf,0,readSize);
        out.write(buf,0,readSize);
      }
    }
  }


  
  private final byte[] readTo(byte marker)
    throws IOException
  {
    _buf.clear();
    int ret;
    while ( (ret=_in.read())>-1)
    { 
      _readPos++;
      if (ret==marker)
      { return _buf.toByteArray();
      }
      else
      { _buf.append((byte) ret);
      }
    }
    throw new IOException("Unexpected end of response reading to "+marker);
  }

  private final byte[] readHeaderName()
    throws IOException
  {
    _buf.clear();
    int ret;
    while ( (ret=_in.read())>-1)
    { 
      _readPos++;
      switch (ret)
      {
        case 32:
          return _buf.toByteArray();
        case 13:
          expect((byte) 10);
          return _buf.toByteArray();
        default:
          _buf.append((byte) ret);
      }
    }
    throw new IOException("Unexpected end of response reading header name");
  }

  private final void skipTo(byte marker)
    throws IOException
  {
    int ret;
    while ( (ret=_in.read())>-1)
    { 
      _readPos++;
      if (ret==marker)
      { return;
      }
    }
    if (ret==-1)
    { throw new IOException("Unexpected end of response skipping to "+marker);
    }
  }

  private final void expect(byte marker)
    throws IOException
  {
    int ret;
    _readPos++;
    if ( (ret=_in.read())!=marker)
    { throw new IOException("Unexpected character in response "+ret+" at "+_readPos);
    }
  }
  
	private final byte[] readToSpace()
    throws IOException
	{ return readTo((byte) 32);
	}
	
	private final byte[] readToEOL()
    throws IOException
	{ 
    byte[] ret=readTo((byte) 13);
    expect((byte) 10);
    return ret;
	}
	
	private final void skipToEOL()
    throws IOException
	{
    skipTo((byte) 13);
    expect((byte) 10);
	}
	

	private final void readHeaders()
    throws IOException
	{
		_cookies=new LinkedList<Cookie>();
    readStatus();
    // Deal with 100, 101 result codes- no headers.
    while (readHeader()) {}

	}	

  private final void readContent(OutputStream out)
    throws IOException
  { 
    // XXX Deal with Content-Length, chunked encoding
    StreamUtil.copyRaw(_in,out,8192);
  }
  
  private final void readStatus()	
    throws IOException
  { 
    _protocol=new String(readToSpace());
    _resultCode=Integer.parseInt(new String(readToSpace()));
    _resultMessage=new String(readToEOL());
  }
  
 
	private final boolean readHeader()
    throws IOException
	{	
		byte[] name=readHeaderName();
    if (name.length==0)
    { return false;
    }
    if (_parseAllHeaders)
    {
      byte[] val=readToEOL();
      if (compareBytes(name,_cookieHeader))
      { readCookie(val);
      }
      _headerList.add(toString(name,name.length-1));
      _headerMap.put(toString(name,name.length-1),toString(val,val.length));
    }
    else
    {
      if (compareBytes(name,_cookieHeader))
      { 
        byte[] val=readToEOL();
        readCookie(val);
      }
      else
      { skipToEOL();
      }
    }
		return true;
	}
	
	private boolean compareBytes(byte[] first,byte[] second)
	{
		if (first==second)
		{ return true;
		}
		else if (first.length!=second.length)
		{ return false;
		}
		else
		{
			for (int i=0;i<first.length;i++)
			{
				if (first[i]!=second[i])
				{ return false;
				}
			}
			return true;
		}
	}
	
	private final void readCookie(byte[] val)
	{
		Cookie cookie=new Cookie();
		cookie.setClientResponseHeader(val);
		_cookies.add(cookie);
	}
			
	public List<Cookie> getCookies()
	{	return _cookies;
	}
}
