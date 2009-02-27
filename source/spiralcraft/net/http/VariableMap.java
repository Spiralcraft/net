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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes and decodes application/x-www-form-urlencoded urlencoded form and 
 *   query variables
 */
public class VariableMap
  extends ListMap<String,String>
{

  /**
   * <p>Construct a Variable map from a URL encoded string of the 
   *   application/x-form-urlencoded content type.
   * </p>
   * 
   * @param map
   */
  public static final VariableMap fromUrlEncodedString(String encodedForm)
  {
    VariableMap map=new VariableMap();
    map.parseEncodedForm(encodedForm);
    return map;
  }

  /**
   * <p>Construct a Variable map using a standard Map from String keys to
   *  String arrays of values.
   * </p>
   * 
   * @param map
   */
  public VariableMap(Map<String,String[]> map)
  { 
    super(new LinkedHashMap<String,List<String>>());
    for (Map.Entry<String,String[]> entry: map.entrySet())
    {
      for (String value:entry.getValue())
      { add(entry.getKey(),value);
      }
    }
    
  }
  
  
  /**
   * <p>Construct an empty VariableMap
   * </p>
   * 
   * @param map
   */
  public VariableMap()
  { super(new LinkedHashMap<String,List<String>>());
  }

  /**
   * <p>Add values to the map from a URL encoded string of the 
   *   application/x-form-urlencoded content type.
   * </p>
   * 
   * @param encodedForm
   */
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
  
  /**
   * <p>Create a URL encoded string in the form of the 
   *   application/x-form-urlencoded content type from the values in the map
   * </p>
   * 
   * @param encodedForm
   */
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
  
  public Iterator<String> getNames()
  { return keySet().iterator();
  }
  
  public String[] getValues(String name)
  {
    List<String> ret=get(name);
    if (ret!=null)
    { return ret.toArray(new String[ret.size()]);
    }
    else
    { return null;
    }
  }
  
  @Override
  public void add(String name,String value)
  {
    if (value==null)
    { throw new IllegalArgumentException("Value cannot be null");
    }
    super.add(name, value);
  }
  
  @Override
  public String toString()
  { return super.toString()+":"+generateEncodedForm();
  }
}
