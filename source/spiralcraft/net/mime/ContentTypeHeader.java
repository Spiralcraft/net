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

import java.io.IOException;
import java.io.PushbackReader;
import java.util.LinkedHashMap;

public class ContentTypeHeader
    extends MimeHeader
{

  private String type;
  private String subtype;
  private LinkedHashMap<String,String> parameters;
  
  public ContentTypeHeader(String name,String value)
    throws IOException
  {
    super(name, value);
    parse();
    
  }
  
  public void parse()
    throws IOException
  {
    PushbackReader in=startParse();
    type=extractTokenTo(in,'/');
    int c=in.read();
    if (c!='/')
    { throw new IOException("Expected '/'");
    }
    subtype=extractTokenTo(in,';');
    
    c=in.read();
    if (c==';')
    { 
      in.unread(c);
      parameters=extractParameters(in,null);
    }
    else if (c!=-1)
    { throw new IOException("Found '"+(char) c+"' ("+c+") in an unexpected place");
    }
  }
  
  public String getFullType()
  { return type+"/"+subtype;
  }
  
  public String getParameter(String name)
  { 
    if (parameters==null)
    { return null;
    }
    return parameters.get(name);
  }
}
