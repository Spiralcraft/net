package spiralcraft.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Represents an HTTP cookie
 */
public class Cookie
{
	private String _name;
	private byte[] _nameBytes;
	private byte[] _value;
	private byte[] _path;
	private byte[] _responseHeader;
	
	private int _readPos=0;
	
	public String getName()
	{ return _name;
	}
		
  public String getValue()
  { return _value!=null?new String(_value):null;
  }

  public String getPath()
  { return _path!=null?new String(_path):null;
  }


	public void setName(String name)
	{
		_name=name;
		_nameBytes=name.getBytes();
	}
	
	public void setValue(String value)
	{ _value=value.getBytes();
	}

	public byte[] getClientRequestHeader()
	{
		ByteArrayOutputStream ba=new ByteArrayOutputStream(_nameBytes.length+_value.length+1);
		try
		{
			ba.write(_nameBytes);
			ba.write("=".getBytes());
			ba.write(_value);
		}
		catch (IOException x)
		{ }
		return ba.toByteArray();
	}
	
	
	public final void setClientResponseHeader(String header)
	{ 
		try
		{
			StringTokenizer st=new StringTokenizer(header,";=");
		
			_name=st.nextToken();
			_value=st.nextToken().getBytes();
			
			while (st.hasMoreTokens())
			{
				String partName=st.nextToken();
				String value=st.nextToken();
				if (partName.equals("path"))
				{ _path=value.getBytes();
				}
			}
		}
		catch (Exception x)
		{ System.out.println("Error reading cookie: "+header);
		}
	}
	
	public final void setClientResponseHeader(byte[] header)
	{
		_responseHeader=header;
		_nameBytes=readToEquals();
		_name=new String(_nameBytes);
		_value=readToColonOrEOL();
	}

	public final byte[] readToColonOrEOL()
	{
			int start=_readPos;
  		while (_readPos<_responseHeader.length
  		       && _responseHeader[_readPos]!=59
						 && _responseHeader[_readPos]!=13
						 )
			{ _readPos++;
			}
			byte[] ret=new byte[_readPos-start];
			System.arraycopy(_responseHeader,start,ret,0,ret.length);
			return ret;
	}

	public final byte[] readToEquals()
	{
		final byte[] ret=readTo(_readPos,(byte) 61);
		_readPos++;
		return ret;
	}

	private final byte[] readTo(final int start,final byte lookfor)
	{
		advanceTo(lookfor);
		byte[] ret=new byte[_readPos-start];
		System.arraycopy(_responseHeader,start,ret,0,ret.length);
		return ret;
	}
	
	private final void advanceTo(final byte b)
	{
		while (_responseHeader[_readPos]!=b)
		{ _readPos++;
		}
	}


}
