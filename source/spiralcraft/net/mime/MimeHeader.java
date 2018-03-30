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
import java.util.HashMap;

import spiralcraft.log.ClassLog;
import spiralcraft.text.CaseInsensitiveString;
import spiralcraft.util.string.StringConverter;
import spiralcraft.util.string.StringPool;



public abstract class MimeHeader
{
  
  static
  { 
    StringConverter
      .registerInstance(MimeHeader.class, new MimeHeaderToString());
  }
  
  protected static final ClassLog log
    =ClassLog.getInstance(MimeHeader.class);
  
  private static final HashMap<CaseInsensitiveString,HeaderFactory> factories
    =new HashMap<>();
  
  protected static void register(String name,HeaderFactory factory)
  { factories.put(MimeHeaderMap.mapCase(name),factory);
  }
  
  public static MimeHeader parse(String header,String quotableChars)
    throws IOException
  {
    int colonPos=header.indexOf(":");
    if (colonPos<0)
    { throw new IOException("':' in wrong place "+colonPos+" in "+header);
    }
        
    String name=header.substring(0,colonPos).trim();
    String value=header.substring(colonPos+1).trim();
    
    HeaderFactory factory=factories.get(MimeHeaderMap.mapCase(name));
    if (factory!=null)
    { return factory.parse(StringPool.INSTANCE.get(name),value,quotableChars);
    }
    else
    { return new GenericHeader(name,value);
    }
  }
  
  
  
  private String name;
  private String rawValue;
    
  protected MimeHeader(String name,String rawValue)
  {
    this.name=name;
    this.rawValue=rawValue;
  }
  
  public String getName()
  { return name;
  }
  
  /**
   * 
   * @return The unparsed value
   */
  public String getRawValue()
  { return rawValue;
  }

  
  protected HeaderReader startParse()
  { return new HeaderReader(rawValue);
  }
  
  @Override
  public String toString()
  { return this.name+": "+this.rawValue; 
  }


}
