//
// Copyright (c) 2009 Michael Toth
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
package spiralcraft.net.xmlschema.types;

import spiralcraft.data.TypeResolver;

import java.net.URI;

/**
 * Extends the standard BooleanType to read "1" and "0" as valid data.
 */
public class BooleanType
  extends spiralcraft.data.types.standard.BooleanType
{
  public BooleanType(TypeResolver resolver,URI uri)
  { super(resolver,uri);
  }
  
  @Override
  public Boolean fromString(String str)
  { 
    if (str==null || str.isEmpty())
    { return null;
    }
    return str.equals("1") || str.equals("true");
    
  }
}