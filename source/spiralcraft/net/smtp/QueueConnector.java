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

import spiralcraft.common.ContextualException;
import spiralcraft.data.DataException;
import spiralcraft.data.RuntimeDataException;
import spiralcraft.data.Space;
//import spiralcraft.data.Space;
import spiralcraft.data.Tuple;
import spiralcraft.data.Type;
import spiralcraft.data.editor.TupleEditor;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.session.BufferTuple;
import spiralcraft.data.session.DataSession;
import spiralcraft.lang.Contextual;
import spiralcraft.lang.Focus;
//import spiralcraft.lang.util.LangUtil;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.util.LangUtil;
import spiralcraft.log.ClassLog;

public class QueueConnector
  implements SMTPConnector,Contextual
{

  private final ClassLog log=ClassLog.getInstance(getClass());
  
  private TupleEditor editor;
  // private Space space;
  private Type<Tuple> queueType;
  private Type<Envelope> envelopeType;
  private final ThreadLocalChannel<DataSession> dsChannel
    =new ThreadLocalChannel<DataSession>
      (BeanReflector.<DataSession>getInstance(DataSession.class));
  private Space space;
  private boolean debug;
  
  
  @Override
  public void send(
    Envelope envelope)
    throws IOException
  {
    DataSession ds=new DataSession();
    ds.setDebug(true);
    ds.setSpace(space);
    dsChannel.push(ds);
    editor.push();
    try
    { 
      editor.initBuffer();
      BufferTuple envelopeBuffer=dsChannel.get().newBuffer(Type.getBufferType(envelopeType));
      envelopeBuffer.copyFrom(envelopeType.toData(envelope).asTuple());
      editor.getBuffer().set
        ("envelope"
        ,envelopeBuffer
        );
      if (debug)
      { log.fine("Queuing: "+editor.getBuffer().toString());
      }
      editor.save(true);
    }
    catch (DataException x)
    { throw new IOException("Error accessing queue",x);
    }
    finally
    { 
      editor.pop();
      dsChannel.pop();
    }
    
    if (debug)
    { log.fine("Queued "+envelope);
    }
  }

  public void setDebug(boolean debug)
  { this.debug=debug;
  }

  
  @SuppressWarnings("unchecked")
  @Override
  public Focus<?> bind(
    Focus<?> focusChain)
    throws ContextualException
  {
    space=LangUtil.assertInstance(Space.class,focusChain);
    try
    {
      if (queueType==null)
      { queueType=Type.resolve("class:/spiralcraft/net/smtp/Queue");
      }

      if (envelopeType==null)
      { envelopeType=Type.resolve("class:/spiralcraft/net/smtp/Envelope");
      }
    }
    catch (DataException x)
    { throw new RuntimeDataException("Error binding queue type",x);
    }

    focusChain=focusChain.chain(dsChannel);
    
    editor=new TupleEditor();
    editor.setAutoCreate(true);
    
    focusChain
      =editor.bind
        (focusChain.chain
          (DataReflector.getInstance(queueType).createNilChannel())
        );
    
    // space=LangUtil.findInstance(Space.class, focusChain);
    
    
    return focusChain;
  }

}
