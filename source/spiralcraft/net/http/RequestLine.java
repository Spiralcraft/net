//
// Copyright (c) 1998,2011 Michael Toth
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

import java.net.URI;

import spiralcraft.common.Immutable;
import spiralcraft.text.ParseException;
import spiralcraft.text.ParsePosition;
import spiralcraft.util.string.StringUtil;

/**
 * <p>The HTTP/1.1 RFC2616 Request-Line construct
 * </p>
 * 
 * @author mike
 *
 */
@Immutable
public class RequestLine
{
  private final String method;
  private final URI uri;
  private final String version;
  
  public RequestLine(String line)
    throws ParseException
  { 
    String[] parts=StringUtil.explode(line,' ',(char) 0,3);
    if (parts.length!=3)
    { 
      throw new ParseException
        ("Invalid request syntax",new ParsePosition());
    }
    method=parts[0];
    uri=URI.create(parts[1]);
    version=parts[2];
  }
  
  public RequestLine(String method,URI uri,String version)
  { 
    this.method=method;
    this.uri=uri;
    this.version=version;
  }
  
  public String getMethod()
  { return method;
  }
  
  public URI getURI()
  { return uri;
  }
  
  public String getVersion()
  { return version;
  }
  
  @Override
  public String toString()
  { return method+' '+(uri!=null?uri:"*")+' '+version;
  }
}
