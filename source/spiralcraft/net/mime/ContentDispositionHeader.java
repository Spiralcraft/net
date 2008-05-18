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

public class ContentDispositionHeader
    extends MimeHeader
{

  private String dispositionType;
  private LinkedHashMap<String,String> parameters
    =new LinkedHashMap<String,String>();
  
  public ContentDispositionHeader(String name,String value)
    throws IOException
  {
    super(name, value);
    parse();
  }
  
  public void parse()
    throws IOException
  {
    PushbackReader in=startParse();
    dispositionType=extractTokenTo(in,';');
    
    
    int c=in.read();
    if (c==';')
    { parameters=extractParameters(in);
    }
    else
    { throw new IOException("Found '"+c+"' in an unexpected place");
    }
  }
  
  public String getDispositionType()
  { return dispositionType;
  }
  
  public String getParameter(String name)
  { 
    if (parameters==null)
    { return null;
    }
    return parameters.get(name);
  }
}
