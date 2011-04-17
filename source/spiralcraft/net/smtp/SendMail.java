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
package spiralcraft.net.smtp;

import java.io.IOException;
import java.net.URI;

import spiralcraft.lang.Assignment;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.lang.Setter;
import spiralcraft.lang.reflect.BeanReflector;
import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.task.Chain;
import spiralcraft.task.Task;
import spiralcraft.util.ArrayUtil;

/**
 * <p>Scenario to send an email.
 * </p>
 * 
 * <p>Chained child scenarios are provided access to the message 
 *   Envelope before it is sent
 * </p>
 * 
 * @author mike
 *
 */
public class SendMail
  extends Chain<Void,Void>
{

  private Channel<SMTPConnector> smtpChannel;
  private SMTPConnector smtpConnector;
  
  private Assignment<?>[] postAssignments;
  private Setter<?>[] postSetters;

  private Assignment<?>[] preAssignments;
  private Setter<?>[] preSetters;

  private ThreadLocalChannel<Envelope> envelopeChannel
    =new ThreadLocalChannel<Envelope> 
      (BeanReflector.<Envelope>getInstance(Envelope.class));
  
  
  private Expression<String> senderX;
  private Expression<String> recipientX;
  private Expression<String[]> recipientsX;
  private Expression<String> messageX;
  
  private HeaderBinding<?>[] headerBindings;

  

  /**
   * The Sender expression, evaluated against the focus chain ending in
   *   the message Envelope
   * 
   * @param senderX
   */
  public void setSenderX(Expression<String> senderX)
  { this.senderX=senderX;
  }
  
  /**
   * The recipient expression, evaluated against the focus chain ending in
   *   the message Envelope
   * 
   * @param senderX
   */
  public void setRecipientX(Expression<String> recipientX)
  { this.recipientX=recipientX;
  }

  /**
   * The multiple recipients expression, evaluated against the focus chain 
   *   ending in the message Envelope
   * 
   * @param senderX
   */
  public void setRecipientsX(Expression<String[]> recipientsX)
  { this.recipientsX=recipientsX;
  }

  /**
   * The message expression, evaluated against the focus chain 
   *   ending in the message Envelope
   * 
   * @param senderX
   */
  public void setMessageX(Expression<String> messageX)
  { this.messageX=messageX;
  }
    
  /**
   * <p>Assignments which get executed prior to a send attempt (eg. to resolve
   *   credentials)
   * </p>
   * 
   * @param assignments
   */
  public void setPreAssignments(Assignment<Object>[] assignments)
  { this.preAssignments=assignments;
  }  

  /**
   * <p>Assignments which get executed immediately after a successful send
   * </p>
   * 
   * <p>XXX refactor to setPostAssignments()
   * </p>
   * 
   * @param assignments
   */
  public void setAssignments(Assignment<Object>[] assignments)
  { this.postAssignments=assignments;
  }  

  /**
   * <p>Assignments which get executed immediately after a successful send
   * </p>
   * 
   * <p>XXX refactor to setPostAssignments()
   * </p>
   * 
   * @param assignments
   */
  public void setPostAssignments(Assignment<Object>[] assignments)
  { this.postAssignments=assignments;
  }  
  
  public void setHeaderBindings(HeaderBinding<?>[] headerBindings)
  { this.headerBindings=headerBindings;
  }
    
  public void setSmtpConnector(SMTPConnector smtpConnector)
  { this.smtpConnector=smtpConnector;
  }
  
  @Override
  protected Task task()
  { return new SendMailTask();
  }

  
  class SendMailTask
    extends ChainTask
  {

    @Override
    protected void work() throws InterruptedException
    {
      Envelope envelope=new Envelope();
      envelopeChannel.push(envelope);
      try
      {
        Setter.applyArray(preSetters);
        super.work();

        if (envelope.getSenderMailAddress()==null)
        { this.addException(new Exception("Envelope missing sender address"));
        }
        else if 
          (envelope.getRecipientList()==null || envelope.getRecipientList().isEmpty())
        { this.addException(new Exception("Envelope missing recipients"));
        }
        else if
          (envelope.getEncodedMessage()==null)
        { this.addException(new Exception("Envelope missing message text"));
        }
        else
        {
          try
          {
            if (headerBindings!=null)
            { envelope.insertHeaders(headerBindings,true);
            }
            
            if (smtpChannel!=null)
            { smtpChannel.get().send(envelope);
            }
            else if (smtpConnector!=null)
            { smtpConnector.send(envelope);
            }
            else
            { throw new IllegalStateException("No SMTPConnector available");
            }
            
            Setter.applyArray(postSetters);
            if (debug)
            {  
              log.fine
                ("Sent to "+envelope.getRecipientList()
                +"\r\n"+envelope.getEncodedMessage()
                );
            }
          }
          catch (IOException x)
          { this.addException(x);
          }
      
        }
      }
      finally
      { envelopeChannel.pop();
      }
      
    }
  }  

  

  @Override
  protected void bindChildren(Focus<?> focusChain)
    throws BindException
  {
    Focus<SMTPConnector> smtpFocus
      =focusChain.<SMTPConnector>
        findFocus(URI.create("class:/spiralcraft/net/smtp/SMTPConnector"));
    if (smtpFocus!=null)
    { smtpChannel=smtpFocus.getSubject();
    } 
    
    
    
    focusChain=focusChain.telescope(envelopeChannel);
    
    if (senderX!=null)
    { addPreAssignment(".sender",senderX);
    }
    
    if (recipientX!=null)
    { addPreAssignment(".recipient",recipientX);
    }

    if (recipientsX!=null)
    { addPreAssignment(".recipients",recipientsX);
    }
    
    if (messageX!=null)
    { addPreAssignment(".encodedMessage",messageX);
    }

    postSetters=Assignment.bindArray(postAssignments,focusChain);
    preSetters=Assignment.bindArray(preAssignments,focusChain);
    if (headerBindings!=null)
    {
      for (HeaderBinding<?> binding: headerBindings)
      { binding.bind(focusChain);
      }
    }
    super.bindChildren(focusChain);
        

  }    

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void addPreAssignment(String targetX,Expression source)
  { 
    Assignment<?> assignment
      =new Assignment
        (Expression.create(targetX)
        ,source
        );
    assignment.setDebug(this.debug);
    preAssignments
      =preAssignments!=null
      ?ArrayUtil.append
        (preAssignments
        ,assignment
        )
       :new Assignment[] {assignment}
       ;
    
  }
      


  

}
