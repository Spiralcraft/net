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
import spiralcraft.sax.ElementRenderer;

public class Channel
  extends ElementRenderer
{

  private Expression<String> titleX;
  private Expression<String> linkX;
  private Expression<String> descriptionX;
  private Expression<String> languageX;
  private Expression<Date> pubDateX;
  private Expression<String> lastBuildDateX;
  private Expression<String> docsX;
  private Expression<String> generatorX;
  private Expression<String> managingEditorX;
  private Expression<String> webMasterX;
  
  @Override
  public String getLocalName()
  { return "channel";
  }
  
    @Override
  public Focus<?> bind(Focus<?> parentFocus)
    throws BindException
  {
    if (titleX==null) 
    { throw new BindException("title is required ");
    }
    if (linkX==null) 
    { throw new BindException("link is required");
    }
    if (descriptionX==null) 
    { throw new BindException("description is required");
    }
    
    addLeaf("title",titleX);
    addLeaf("link", linkX);
    addLeaf("description", descriptionX);
    addLeaf("language", languageX);
    addLeaf("pubDate", pubDateX);
    addLeaf("lastBuildDate", lastBuildDateX);
    addLeaf("docs", docsX);
    addLeaf("generator", generatorX);
    addLeaf("managingEditor", managingEditorX);
    addLeaf("webMaster", webMasterX);    

    return super.bind(parentFocus);
  }


  public void setTitleX(Expression<String> titleX)
  { this.titleX = titleX;
  }

  public void setTitle(String title)
  { this.titleX = Expression.literal(title);
  }

  public void setLinkX(Expression<String> linkX)
  { this.linkX = linkX;
  }

  public void setLink(String link)
  { this.linkX = Expression.literal(link);
  }

  public void setDescriptionX(Expression<String> descriptionX)
  { this.descriptionX = descriptionX;
  }

  public void setDescription(String description)
  { this.descriptionX = Expression.literal(description);
  }

  public void setLanguageX(Expression<String> languageX)
  { this.languageX = languageX;
  }

  public void setLanguage(String language)
  { this.languageX = Expression.literal(language);
  }

  public void setPubDateX(Expression<Date> pubDateX)
  { this.pubDateX = pubDateX;
  }


  public void setLastBuildDateX(Expression<String> lastBuildDateX)
  { this.lastBuildDateX = lastBuildDateX;
  }


  public void setDocsX(Expression<String> docsX)
  { this.docsX = docsX;
  }


  public void setGeneratorX(Expression<String> generatorX)
  { this.generatorX = generatorX;
  }


  public void setManagingEditorX(Expression<String> managingEditorX)
  { this.managingEditorX = managingEditorX;
  }

  public void setManagingEditor(String managingEditor)
  { this.managingEditorX = Expression.literal(managingEditor);
  }

  public void setWebMasterX(Expression<String> webMasterX)
  { this.webMasterX = webMasterX;
  }
  
  public void setWebMaster(String webMaster)
  { this.webMasterX = Expression.literal(webMaster);
  }
  
}
