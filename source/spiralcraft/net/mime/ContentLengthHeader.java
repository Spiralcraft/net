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

public class ContentLengthHeader
    extends MimeHeader
{

  public static final String NAME="Content-Length";
  
  static
  { 
    register
      (NAME
      ,new HeaderFactory()
        {
          @Override
          public MimeHeader parse(String name,String value,String quotable)
            throws IOException
          { return new ContentLengthHeader(NAME,value);
          }
        }
      );
    
  }
  
  /**
   * Tickle the static initializer 
   */
  public static final void init() {};
  
  private long length;
  
  public ContentLengthHeader(String name,String value)
  {
    super(name, value);
    length=Long.parseLong(value);
  }
  
  public ContentLengthHeader(long length)
  { 
    super(NAME,Long.toString(length));
    this.length=length;
  }
  
  public long getLength()
  { return length;
  }
  
}
