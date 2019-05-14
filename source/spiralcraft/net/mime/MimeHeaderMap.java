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
import java.util.List;
import java.util.Map;

import spiralcraft.text.CaseInsensitiveString;
import spiralcraft.util.ListMap;
import spiralcraft.util.string.StringPool;

public class MimeHeaderMap
  extends ListMap<CaseInsensitiveString,MimeHeader>
{
  
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 1L;
  
  private static final HashMap<String,CaseInsensitiveString> caseMap= 
    new HashMap<>();
    
  protected static final CaseInsensitiveString mapCase(String in)
  {
    CaseInsensitiveString ret=caseMap.get(in);
    if (ret!=null)
    { return ret;
    }
    synchronized(caseMap)
    {
      ret=caseMap.get(in);
      if (ret!=null)
      { return ret;
      }
      ret=new CaseInsensitiveString(in);
      caseMap.put(in,ret);
      return ret;
    }
  }
  
  public static final String HDR_CONTENT_TYPE="Content-Type";
  public static final String HDR_CONTENT_DISPOSITION="Content-Disposition";
  
  private String quotableChars;
  
  public ContentDispositionHeader getContentDisposition()
  { return (ContentDispositionHeader) getFirst(mapCase(HDR_CONTENT_DISPOSITION));
  }

  public ContentTypeHeader getContentType()
  { return (ContentTypeHeader) getFirst(mapCase(HDR_CONTENT_TYPE));
  }
  
  public void setQuotableChars(String quotableChars)
  { this.quotableChars=quotableChars;
  }
  
  public void add(MimeHeader header)
  { add(mapCase(header.getName()),header);
  }
  
  public void put(MimeHeader header)
  { 
    remove(mapCase(header.getName()));
    add(mapCase(header.getName()),header);
  }
  
  public List<MimeHeader> getHeaders(String name)
  { return get(mapCase(name));
  }
  
  public MimeHeader getHeader(String name)
  { return getFirst(mapCase(name));
  }
  
  public boolean containsHeader(String name)
  { return containsKey(mapCase(name));
  }
  
  public List<MimeHeader> removeHeaders(String name)
  { return remove(mapCase(name));
  }

  /**
   * Return a map of header names in simple String form to lists of values in
   *   String form
   */
  public Map<String,List<String>> toStringListMap()
  { 
    ListMap<String,String> map= new ListMap<>();
    for (CaseInsensitiveString key: keySet())
    { 
      for (MimeHeader header: get(key))
      { map.add(key.toString(), header.getRawValue());
      }
    }
    return map;
  }
  
  public void addRawHeader(String header)
    throws IOException
  {
    int colonPos=header.indexOf(":");
    if (colonPos<0)
    { throw new IOException("':' in wrong place "+colonPos+" in "+header);
    }
        
    String name=StringPool.INSTANCE.get(header.substring(0,colonPos).trim());
    String value=header.substring(colonPos+1);
    
    if (name.equals(HDR_CONTENT_TYPE))
    { add(mapCase(HDR_CONTENT_TYPE),new ContentTypeHeader(name,value));
    }
    else if (name.equals(HDR_CONTENT_DISPOSITION))
    { 
      add
        (mapCase(HDR_CONTENT_DISPOSITION)
        ,new ContentDispositionHeader(name,value,quotableChars)
        );
    }
    else
    { add(mapCase(name),new GenericHeader(name,value));
    }
    
  }
  
  
}
