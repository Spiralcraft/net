//
// Copyright (c) 2009,2009 Michael Toth
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
package spiralcraft.net.rss;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;

import spiralcraft.sax.ElementRenderer;

/**
 * <p>Renders an RSS Feed container
 * </p>
 * 
 * @author mike
 *
 */
public class Feed
    extends ElementRenderer
{
  
  { document=true;
  }
  
  @Override
  public String getLocalName()
  { return "rss";
  }
  
  public String getContentType()
  { return "application/rss+xml;charset=UTF-8";
  }
  
  @Override
  public Focus<?> bind(Focus<?> parentFocus)
    throws BindException
  {
    addAttributeBinding("version",Expression.create("\"2.0\""));
    return super.bind(parentFocus);
  }
  

}
