//
// Copyright (c) 1998,2008 Michael Toth
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
package spiralcraft.net.mime;

import java.io.InputStream;
import java.io.IOException;

import spiralcraft.vfs.StreamUtil;

import spiralcraft.io.NullOutputStream;

import spiralcraft.io.WindowInputStream;
import spiralcraft.log.ClassLog;
import spiralcraft.log.Level;

import spiralcraft.util.string.StringUtil;

/**
 * Parses Multipart Mime request data
 */
public class MultipartParser
{

  private static final boolean DEBUG=false;
  private static final ClassLog log
    =ClassLog.getInstance(MultipartParser.class);
  
  private MimeHeaderMap _headers;
  private String quotableChars=null;


  
  public MultipartParser(InputStream in,String contentType,int contentLength)
    throws IOException
  {
    _contentType=contentType;
    _contentLength=contentLength;
    if (DEBUG)
    { log.fine("Content Length= "+contentLength);
    }

    if (_contentType==null)
    { 
      _done=true;
      return;
    }

    if (_contentLength<=0)
    { 
      _done=true;
      return;
    }

    resolveContentType();

    if (_separator==null)
    { 
      _done=true;
      return;
    }

    _in=new MultipartInputStream(in,StringUtil.asciiBytes(_separator),_contentLength);

    // Discard first separator
    _count+=StreamUtil.copyRaw
      (_in,new NullOutputStream(),8192,_contentLength-_count);
    
    if (DEBUG)
    { log.fine("Separator ("+_count+")");
    }

  }

  /**
   * Restrict the set of quotable chars at critical points. The sole purpose
   *   is to work around IE sending unqoted backslashes.
   * 
   * @param quotableChars
   */
  public void setQuotableChars(String quotableChars)
  { this.quotableChars=quotableChars;
  }
  
  public void resolveContentType()
  { _separator=_contentType.substring(_contentType.indexOf("boundary=")+9);
  }


  
  /**
   * Advances the Parser to the next Mime part
   */
  public boolean nextPart()
    throws IOException
  {
    if (DEBUG)
    { log.fine("nextPart()");
    }

    if (_done)
    { 
      if (DEBUG)
      { log.fine("nextPart(): _done=true");
      }
      return false;
    }

    if (_partInputStream!=null && !_drained)
    { 
      if (DEBUG)
      { System.err.println("nextPart(): discarding part");
      }
      StreamUtil.copyRaw
        (_partInputStream,new NullOutputStream(),8192,_contentLength-_count);
    }

    if (_count<_contentLength)
    { 

      try
      { readHeaders();
      }
      catch (IOException x)
      {
        if (DEBUG)
        { log.log(Level.FINE,"Error reading headers",x);
        }
        x.printStackTrace();
        throw x;
      }
      
      if (!_done)
      {
        _partInputStream=new PartInputStream(_in,_separator.length());
        return true;
      }
      else
      { 
        if (DEBUG)
        { log.fine("nextPart(): done after readHeaders()");
        }
      }
    }
    else
    { 
      if (DEBUG)
      { log.fine("nextPart(): reached content length");
      }
    }
    return false;
  }

  public MimeHeader getHeader(String name)
  { return _headers.get(name);
  }

  public String getPartName()
  {
    ContentDispositionHeader header=_headers.getContentDisposition();
    if (header!=null)
    { return header.getParameter("name");
    }
    else
    { return null;
    }
  }

  public String getPartFilename()
  {
    ContentDispositionHeader header=_headers.getContentDisposition();
    if (header!=null)
    { return header.getParameter("filename");
    }
    else
    { return null;
    }
  }

  public String getPartContentType()
  {
    ContentTypeHeader header=_headers.getContentType();
    if (header!=null)
    { return header.getFullType();
    }
    else
    { return null;
    }
  }

  public InputStream getInputStream()
  { return _partInputStream;
  }

  private InputStream _in;
  private InputStream _partInputStream;
  private boolean _drained;
  private boolean _done;
  private String _contentType;
  private int _contentLength;
  private int _count;
  private String _separator;

  private void readHeaders()
    throws IOException
  {
    _headers=new MimeHeaderMap();
    _headers.setQuotableChars(quotableChars);
    
    boolean first=true; 
    // Read Headers
    StringBuilder header=new StringBuilder();
    while (true)
    {
      String line=StreamUtil.readAsciiLine(_in,null,_contentLength-_count);
      
      if (DEBUG)
      { log.fine("line ("+(line.length()+2)+"): "+line);
      }
      _count+=line.length()+2;
      if (line.equals("--"))
      { 
        _done=true;
        return;
      }
      if (line.length()==0)
      {
        if (first)
        {
          // Expect a first CR if not at end
          first=false;
        }
        else
        { 
          // Last header
          if (header.length()>0)
          { _headers.parseHeader(header.toString());
          }
          break;
        }
      }
      else
      { 
        if (line.startsWith(" ")
            || line.startsWith("\t")
           )
        { header.append(line.substring(1));
        }
        else
        {
          if (header.length()>0)
          { _headers.parseHeader(header.toString());
          }
          header.setLength(0);
          header.append(line);
        }
          
      }
    }
  }

  class PartInputStream
    extends WindowInputStream
  {
    private boolean _reading;

    public PartInputStream(InputStream in,int len)
      throws IOException
    {
      // Window covers CRLF at beginning and end of separator
      super(in,len+4,false);
      _drained=false;
    }

    @Override
    public int read()
      throws IOException
    { 
      if (_reading)
      { 
        // XXX Stack is inefficient here
        return super.read();
      }

      int val=super.read();
      if (val>=0)
      { 
        _count++;
        if (DEBUG)
        { System.err.println("Chunk (1): "+(char) val);
        }
      }
      else
      { _drained=true;
      }
      return val;
    }

    @Override
    public int read(byte[] b)
      throws IOException
    { return read(b,0,b.length);
    }

    @Override
    public int read(byte[] b,int start,int len)
      throws IOException
    {
      _reading=true;
      try
      {
        int count=super.read(b,start,len);
        _reading=false;
  
        if (count>=0)
        { 
          if (DEBUG)
          { System.err.println("Chunk ("+count+"): "+new String(b,start,count));
          }
          _count+=count;
        }
        else
        { _drained=true;
        }
        return count;
      }
      finally
      { _reading=false;
      }
    }
  }


}
