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
}
