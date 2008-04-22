//
// Copyright (c) 1998,2005 Michael Toth
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

import spiralcraft.util.ListMap;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Encodes and decodes urlencoded form and query variables
 */
public class VariableMap
  extends ListMap<String,String>
{

  public static final VariableMap fromUrlEncodedString(String encodedForm)
  {
    VariableMap map=new VariableMap();
    map.parseEncodedForm(encodedForm);
    return map;
  }

  public VariableMap()
  { super(new LinkedHashMap<String,List<String>>());
  }

  public void parseEncodedForm(String encodedForm)
  { 
    String[] pairs=encodedForm.split("&");
    for (String pair: pairs)
    {
      int eqpos=pair.indexOf('=');
      if (eqpos>0 && eqpos<pair.length()-1)
      {
        String name=URLCodec.decode(pair.substring(0,eqpos));
        String[] values=pair.substring(eqpos+1).split(",");
        for (String value: values)
        { add(name,URLCodec.decode(value));
        }
      }
    }
    
  }
  
  public String generateEncodedForm()
  { 
    StringBuilder buf=new StringBuilder();
    boolean first=true;
    for (Entry<String,List<String>> entry: entrySet())
    {
      if (first)
      { first=false;
      }
      else
      { buf.append("&");
      }
      buf.append(URLCodec.encode(entry.getKey()));
      buf.append("=");
      
      boolean first2=true;
      for (String string:entry.getValue())
      {
        if (first2)
        { first2=false;
        }
        else
        { buf.append(",");
        }
        buf.append(URLCodec.encode(string));
      }
    }
    if (buf.length()==0)
    { return null;
    }
    else
    { return buf.toString();
    }
      
  }
  
  public String toString()
  { return super.toString()+":"+generateEncodedForm();
  }
}
