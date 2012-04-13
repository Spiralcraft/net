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
import java.util.LinkedHashMap;

public class ContentDispositionHeader
    extends MimeHeader
{
  
  public static final String NAME="Content-Disposition";
  
  static
  { 
    register
      (NAME
      ,new HeaderFactory()
        {
          @Override
          public MimeHeader parse(String name,String value,String quotable)
            throws IOException
          { return new ContentDispositionHeader(NAME,value,quotable);
          }
        }
      );
    
  }
  
  /**
   * Tickle the static initializer 
   */
  public static final void init() {};

  
  private String dispositionType;
  private LinkedHashMap<String,String> parameters
    =new LinkedHashMap<String,String>();
  
  public ContentDispositionHeader(String name,String value,String quotableChars)
    throws IOException
  {
    super(name, value);
    parse(quotableChars);
  }
  
  public void parse(String quotableChars)
    throws IOException
  {
    HeaderReader in=startParse();
    dispositionType=in.extractTokenTo(';');
    
    
    int c=in.read();
    if (c==';')
    { 
      in.unread(c);
      parameters=in.extractParameters(quotableChars,false);
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
