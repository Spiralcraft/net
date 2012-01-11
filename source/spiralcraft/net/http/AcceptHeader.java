//
// Copyright (c) 2012 Michael Toth
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import spiralcraft.net.mime.HeaderReader;
import spiralcraft.net.mime.MimeHeader;

public class AcceptHeader
    extends MimeHeader
{

  private List<MediaRange> ranges;
  
  public static AcceptHeader fromString(String headerValue)
    throws IOException
  { 
    if (headerValue!=null)
    { return new AcceptHeader("Accept",headerValue);
    }
    else
    { return null;
    }
  }
  
  public AcceptHeader(String name,String value)
    throws IOException
  {
    super(name, value);
    // log.fine(value);
    parse();
    
  }
  
  public boolean accepts(String type,String subtype)
  { 
    for (MediaRange range:ranges)
    {       
      if (type.equals(range.type)
          && ("*".equals(range.subtype) 
             || subtype.equals(range.subtype)
             )
         )
      { return true;
      } 
      else if ("*".equals(range.type))
      { return true;
      }
    }
    return false;
  }
  
  private void parse()
    throws IOException
  {
    HeaderReader in=startParse();
    ranges=new LinkedList<MediaRange>();
    
    do 
    { 
      //log.fine("parse range");
      ranges.add(readRange(in));
      //log.fine("done #"+ranges.size());
      
    }
    while (in.isContinued(','));
  }
  
  private MediaRange readRange(HeaderReader in)
    throws IOException
  {

    MediaRange range=new MediaRange();
    
    range.type=in.extractTokenTo('/');
    in.read();
    range.subtype=in.extractTokenTo(";,");

    if (!in.isEOF())
    { 
      int c=in.read();
      in.unread(c);
      if (c==';')
      {        
        range.parameters=in.extractParameters(null,true);
        String q=range.parameters.get("q");
        if (q!=null)
        { range.q=Float.parseFloat(q);
        }
      }
    }
    // log.fine(range.type+"/"+range.subtype+";"+range.parameters);
    return range;
  }
}

class MediaRange
{
  String type;
  String subtype;
  float q;
  Map<String,String> parameters;
}

