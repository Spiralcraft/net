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

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.URI;

import java.util.List;
import java.util.Iterator;

import spiralcraft.codec.text.Base64Codec;

import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.StreamUtil;


/**
 * Encapsulates an HTTP request at the client side
 */
public class ClientRequest
{
	
  private byte[] _method="GET".getBytes();
  private byte[] _path="/".getBytes();
  private byte[] _authorization;
  private byte[] _protocol="HTTP/1.0".getBytes();
  private byte[] _accept;
  //private String _acceptLanguage="en-us";
  //private String _acceptEncoding="gzip, deflate";
  //private String _userAgent="Mozilla/4.0 (compatible; Spiralcraft HttpClient 0.1; "+System.getProperty("os.name");
  private byte[] _host;
  private byte[] _connection;
  //private String _referer=null;
  //private String _ifModifiedSince=null;
  //private String _ifNoneMatch=null;
  private byte[] _contentType=null;
  private byte[] _contentLength=null;
  // private List _vars=new LinkedList();
 
  private byte[] _cookies=new byte[0];
  private byte[] _otherHeaders=new byte[0];
  
  private byte[] _eol=new String("\r\n").getBytes();
  private byte[] _sp=new String(" ").getBytes();
  
  private byte[] _headersCache;
  private byte[] _content=new byte[0];

  private int _headerVersion=0;
  private int _headerCacheVersion=-1;

  public final void defaultHeaders()
  {
    _connection="close".getBytes();
    _accept="*/*".getBytes();
    _host="unknown".getBytes();
  }

	public final void setProtocol(String val)
	{
		_protocol=val.getBytes();
		_headerVersion++;
	}

	public final void setPath(String path)
	{
		_path=path.getBytes();
		_headerVersion++;
	}
	
  public final void setMethod(String method)
  {
    _method=method.getBytes();
    _headerVersion++;
  }

	public final void setContentType(String contentType)
	{
		_contentType=contentType.getBytes();
    _headerVersion++;
	}  

  public final void setAuthorization(String authorization)
  { 
    _authorization
      =Base64Codec.encodeAsciiString(authorization).getBytes();
    _headerVersion++;
  }
  
	public final void setContentLength(int val)
	{
		_contentLength=Integer.toString(val).getBytes();
    _headerVersion++;
	}  

  public final void setAccept()
  {
  }

  public final void setHost(String host)
  { _host=host.getBytes();
  }

  public final void setCookies(List<Cookie> cookies)
  {
  	ByteArrayOutputStream out=new ByteArrayOutputStream();

		if (cookies.size()>0)
		{
			try
			{
				Iterator<Cookie> it=cookies.iterator();
				while (it.hasNext())
				{
					Cookie cookie=it.next();
					out.write("Cookie: ".getBytes());
					out.write(cookie.getClientRequestHeader());
					out.write(_eol);
					
				}				
				out.flush();
			}
			catch (IOException x)
			{ }
				
		}
		_cookies=out.toByteArray();
  	_headerVersion++;
  }
  
  public final void setHeaders(List<String> headers)
  {
  	ByteArrayOutputStream out=new ByteArrayOutputStream();
		if (headers.size()>0)
		{
			try
			{
				Iterator<String> it=headers.iterator();
				while (it.hasNext())
				{
					out.write(it.next().getBytes());
					out.write(_eol);
						
				}
				out.flush();
			}
			catch (IOException x)
			{ }
				
		}
  	
  	_otherHeaders=out.toByteArray();
  	_headerVersion++;
  }

  public void setContentURI(URI uri)
    throws IOException
  {
    byte[] content
      =StreamUtil.readBytes
        (Resolver.getInstance().resolve(uri)
          .getInputStream()
        );
  
    setContent(content);
    setContentLength(content.length);
    setMethod("POST");
    
  }
  
  public final void setContent(byte[] content)
  {
  	_content=content;
  	_headerVersion++;
  }
  
  public final void updateHeadersCache()
  {
  	if (_headerVersion>_headerCacheVersion)
  	{
  		try
  		{
				ByteArrayOutputStream out=new ByteArrayOutputStream(1000);
				out.write(_method);
				out.write(_sp);
				out.write(_path);
				out.write(_sp);
				out.write(_protocol);
				out.write(_eol);
				
				if (_accept!=null)
				{		
					out.write("Accept: ".getBytes());
					out.write(_accept);
					out.write(_eol);
				}
				
				if (_host!=null)
				{
					out.write("Host: ".getBytes());
					out.write(_host);
					out.write(_eol);
				}

        if (_authorization!=null)
        { 
          out.write("Authorization: Basic ".getBytes());
          out.write(_authorization);
          out.write(_eol);

        }
        
				if (_connection!=null)
				{
					out.write("Connection: ".getBytes());
					out.write(_connection);
					out.write(_eol);
				}
				
				out.write(_cookies);
				out.write(_otherHeaders);
				
				if (_contentType!=null)
				{
					out.write("Content-Type: ".getBytes());
					out.write(_contentType);
					out.write(_eol);
				}
				
        if (_contentLength!=null)
        {
					out.write("Content-Length: ".getBytes());
					out.write(_contentLength);
					out.write(_eol);
        }
				else if (_content.length>0)
				{
					out.write("Content-Length: ".getBytes());
					out.write(Integer.toString(_content.length).getBytes());
					out.write(_eol);
				}
		
		
				// Header/Body separator
				out.write(_eol);
				out.flush();
				_headersCache=out.toByteArray();
			}
			catch (IOException x)
			{ }
			_headerCacheVersion=_headerVersion;
  	}
  }


  public final void write(OutputStream out)
  	throws IOException
  {
  	updateHeadersCache();
  	out.write(_headersCache);
  	out.write(_content);
  	out.flush();
  }


}
