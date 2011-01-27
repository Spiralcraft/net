//
// Copyright (c) 2011 Michael Toth
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

import java.io.IOException;
import spiralcraft.app.spi.AbstractComponent;
import spiralcraft.data.DataException;
import spiralcraft.data.RuntimeDataException;
import spiralcraft.data.Space;
import spiralcraft.data.Tuple;
import spiralcraft.data.Type;
import spiralcraft.data.editor.TupleEditor;
import spiralcraft.data.reflect.ReflectionType;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Focus;
import spiralcraft.lang.util.LangUtil;

public class SMTPQueue
  implements SMTPConnector,Contextual
{

  
  private TupleEditor editor;
  private Space space;
  private Type<Tuple> queueType;
  
  
  @Override
  public void send(
    Envelope envelope)
    throws IOException
  {
    
    
    
  }

  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
    throws BindException
  {
    try
    {
      if (queueType==null)
      { queueType=Type.resolve("class:/spiralcraft/net/smtp/Queue");
      }

    }
    catch (DataException x)
    { throw new RuntimeDataException("Error binding queue type",x);
    }

    editor=new TupleEditor();
    editor.setAutoCreate(true);
    
    focusChain=editor.bind(focusChain);
    
    space=LangUtil.findInstance(Space.class, focusChain);
    
    
    return focusChain;
  }

}
