//
// Copyright (c) 2010 Michael Toth
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
package spiralcraft.net.smtp;

import java.util.Date;
import java.util.HashMap;

import spiralcraft.lang.ParseException;
import spiralcraft.lang.util.DictionaryBinding;
import spiralcraft.net.syntax.InetTextMessages;
import spiralcraft.util.string.StringConverter;

/**
 * Binds HTTP header names to values
 * 
 * @author mike
 *
 * @param <T>
 */
public class HeaderBinding<T>
  extends DictionaryBinding<T>
{
  @SuppressWarnings("unchecked")
  private static final HashMap<Class,StringConverter> converterMap
    =new HashMap<Class,StringConverter>();

  { converterMap.put(Date.class,InetTextMessages.dateConverter());
  }
  
  public HeaderBinding()
  { setConverterMap(converterMap);
  }
  
  public HeaderBinding(String shorthand)
    throws ParseException
  { 
    super(shorthand);
    setConverterMap(converterMap);
  }
  
  
}
