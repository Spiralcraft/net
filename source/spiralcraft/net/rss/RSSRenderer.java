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

import java.io.IOException;
import java.io.Writer;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Focus;
import spiralcraft.lang.FocusChainObject;
import spiralcraft.text.Renderer;

/**
 * <p>Renders an RSS style feed based on source data obtained from the
 *   FocusChain
 * </p>
 * 
 * @author mike
 *
 */
public class RSSRenderer
  implements Renderer, FocusChainObject
{

  @Override
  public String render()
    throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void render(
    Writer writer)
    throws IOException
  {
  // TODO Auto-generated method stub

  }

  @Override
  public void bind(
    Focus<?> focusChain)
    throws BindException
  {
  // TODO Auto-generated method stub

  }

  @Override
  public Focus<?> getFocus()
  {
    // TODO Auto-generated method stub
    return null;
  }

}
