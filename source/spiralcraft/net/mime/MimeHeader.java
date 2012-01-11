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

import spiralcraft.log.ClassLog;



public abstract class MimeHeader
{
  protected static final ClassLog log
    =ClassLog.getInstance(MimeHeader.class);
  
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


}
