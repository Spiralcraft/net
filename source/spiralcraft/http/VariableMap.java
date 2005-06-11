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
package spiralcraft.http;

import spiralcraft.util.ArrayMap;

import java.util.LinkedHashMap;

/**
 * Encodes and decodes urlencoded form and query variables
 */
public class VariableMap
  extends ArrayMap
{

  public static final VariableMap fromUrlEncodedString(String encodedForm)
  {
    VariableMap map=new VariableMap();
    map.parseEncodedForm(encodedForm);
    return map;
  }

  public VariableMap()
  { super(new LinkedHashMap(),String.class);
  }

  public void parseEncodedForm(String encodedForm)
  {
  }
}
