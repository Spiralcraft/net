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

public class MimeHeaderMap
  extends LinkedHashMap<String,MimeHeader>
{

  private static final long serialVersionUID = 1L;
  
  public static final String HDR_CONTENT_TYPE="Content-Type";
  public static final String HDR_CONTENT_DISPOSITION="Content-Disposition";
  
  private String quotableChars;
  
  public ContentDispositionHeader getContentDisposition()
  { return (ContentDispositionHeader) get("Content-Disposition");
  }

  public ContentTypeHeader getContentType()
  { return (ContentTypeHeader) get("Content-Type");
  }
  
  public void setQuotableChars(String quotableChars)
  { this.quotableChars=quotableChars;
  }
  
  public void parseHeader(String header)
    throws IOException
  {
    int colonPos=header.indexOf(":");
    if (colonPos<0)
    { throw new IOException("':' in wrong place "+colonPos+" in "+header);
    }
        
    String name=header.substring(0,colonPos).trim();
    String value=header.substring(colonPos+1);
    
    if (name.equals(HDR_CONTENT_TYPE))
    { put(HDR_CONTENT_TYPE,new ContentTypeHeader(name,value));
    }
    else if (name.equals(HDR_CONTENT_DISPOSITION))
    { 
      put
        (HDR_CONTENT_DISPOSITION
        ,new ContentDispositionHeader(name,value,quotableChars)
        );
    }
    else
    { 
    }
    
  }
  
}
