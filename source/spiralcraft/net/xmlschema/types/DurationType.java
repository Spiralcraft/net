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

import spiralcraft.data.core.PrimitiveTypeImpl;

import java.net.URI;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationType
  extends PrimitiveTypeImpl<Duration>
{
  public DurationType(TypeResolver resolver,URI uri)
  { super(resolver,uri,Duration.class);
  }
  
  @Override
  public Duration fromString(String str)
  { 
    if (str==null || str.isEmpty())
    { return null;
    }
    try
    { return DatatypeFactory.newInstance().newDuration(str);
    }
    catch (DatatypeConfigurationException x)
    { throw new IllegalArgumentException(x);
    }
  }
}