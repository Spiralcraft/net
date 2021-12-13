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

import spiralcraft.net.mime.HeaderFactory;
import spiralcraft.net.mime.MimeHeader;

public class ConnectionHeader
    extends MimeHeader
{

  public static final String NAME="Connection";
  public static final ConnectionHeader KEEP_ALIVE
    =new ConnectionHeader(NAME,"keep-alive");
  public static final ConnectionHeader CLOSE
    =new ConnectionHeader(NAME,"close");
  
  static
  { 
    register
      (NAME
      ,new HeaderFactory()
        {
          @Override
          public MimeHeader parse(String name,String value,String quotable)
            throws IOException
          { return new ConnectionHeader(NAME,value);
          }
        }
      );
    
  }  

  /**
   * Tickle the static initializer 
   */
  public static final void init() {};

  private boolean keepAlive;
  private boolean close;
  
  public boolean isKeepalive()
  { return keepAlive;
  }
  
  public boolean isClose()
  { return close;
  }
  
  public ConnectionHeader(String name,String value)
  {
    super(name, value);
    if (value.equals("keep-alive"))
    { keepAlive=true;
    }
    else if (value.equals("close"))
    { close=true;
    }
  }
  
}