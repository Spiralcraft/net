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

import java.util.Date;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.DictionaryBinding;
import spiralcraft.net.syntax.InetTextMessages;
import spiralcraft.sax.ElementRenderer;

public class Item
  extends ElementRenderer
{

  private Expression<String> titleX;
  private Expression<String> linkX;
  private Expression<String> descriptionX;
  private Expression<Date> pubDateX;
  private Expression<String> guidX;
  
  @Override
  public String getLocalName()
  { return "item";
  }  
  
    @Override
  public Focus<?> bind(Focus<?> parentFocus)
    throws BindException
  {
    if (titleX==null && descriptionX==null)
    { throw new BindException("One of title or description is required");
    }

    addLeaf("title",titleX);
    addLeaf("link", linkX);
    addLeaf("description", descriptionX);
    
    {
      DictionaryBinding<Date> binding=new DictionaryBinding<Date>();
      binding.setName("pubDate");
      binding.setTarget(pubDateX);
      binding.setConverter(InetTextMessages.dateConverter());
      addLeaf(binding);
    }
    addLeaf("guid", guidX);
    return super.bind(parentFocus);
    
  }


  public void setTitleX(Expression<String> titleX)
  { this.titleX = titleX;
  }


  public void setLinkX(Expression<String> linkX)
  { this.linkX = linkX;
  }


  public void setDescriptionX(Expression<String> descriptionX)
  { this.descriptionX = descriptionX;
  }


  public void setGuidX(Expression<String> guidX)
  { this.guidX = guidX;
  }


  public void setPubDateX(Expression<Date> pubDateX)
  { this.pubDateX = pubDateX;
  }

}
