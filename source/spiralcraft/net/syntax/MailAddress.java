//
// Copyright (c) 1998,2008 Michael Toth
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
package spiralcraft.net.syntax;


import spiralcraft.common.Immutable;
import spiralcraft.text.PushbackParserContext;
import spiralcraft.text.ParseException;

/**
 * <p>Encapsulates an RFC-822 e-mail address, which is composed of a
 *   path preceded by an optional personal name, or a simple path.
 * </p>
 * 
 * @author mike
 *
 */
@Immutable
public class MailAddress
{

  public static final MailAddress[] toAddresses(String[] addresses)
    throws ParseException
  {
    if (addresses==null)
    { return null;
    }
    MailAddress[] maddresses=new MailAddress[addresses.length];
    for (int i=0;i<maddresses.length;i++)
    { maddresses[i]=create(addresses[i]);
    }
    return maddresses;
      
  }


    
  public String smtpPath;
  public String name;
  public String rawAddress;
  
  public static MailAddress create(String address)
    throws ParseException
  { return new MailAddress(address);
  }
  
  protected MailAddress()
  { 
  }
  /**
   * <pre>
   *      mailbox     =  addr-spec                    ; simple address
   *                  /  phrase route-addr            ; name & addr-spec
   * </pre>
   * 
   * @param address
   * @throws ParseException
   */
  public MailAddress(String address)
    throws ParseException
  { 
    this.rawAddress=address.trim();
    
    PushbackParserContext in=startParse();
    name=InetTextMessages.parsePhrase(in);
    int read=in.read();
    if (read!='<')
    { 
      // Phrase ended, or address is in simple form
      name=null;
      in=startParse();
      smtpPath="<"+readSMTPMailbox(in)+">";
    }
    else
    { 
      in.unread((char) read);
      smtpPath=readRouteAddr(in);
    }
  }
  
  @Override
  public String toString()
  { return (name!=null?name+" ":"")+smtpPath;
  }
  
  /**
   * The "&lt;" local-name@domain "&gt;" part of the address.
   * 
   * @return
   */
  public String getSMTPPath()
  { return smtpPath;
  }
  
  /**
   * <pre>
   * Path = "&lt;" [ A-d-l ":" ] Mailbox "&gt;"
   *   A-d-l = At-domain *( "," A-d-l )
   *         ; Note that this form, the so-called "source route",
   *         ; MUST BE accepted, SHOULD NOT be generated, and SHOULD be
   *         ; ignored.
   *   At-domain = "@" domain
   * </pre>
   *
   */
  private String readRouteAddr(PushbackParserContext in)
    throws ParseException
  {
    
    StringBuffer path=new StringBuffer();
    in.expect('<');
    path.append('<');
    
    while (true)
    {
      char chr=(char) in.read();
      if (chr == '@')
      { 
        path.append(chr);
        path.append(Core.parseHostName(in));
        chr=(char) in.read();
        path.append(chr);
        if (chr==',')
        { 
          in.expect('@');
          in.unread('@');
          continue;
        }
        else if (chr==':')
        { break;
        }
        else
        { 
          in.throwParseException
            ("Unexpected char '"+chr+"' in source route");
        }
      }
      else
      { 
        in.unread(chr);
        break;
      }
    }
    
    path.append(readSMTPMailbox(in));
    
    in.expect('>');
    path.append('>');
    
    return path.toString();
    
  }
  
  /**
   * <pre>
   * Mailbox = Local-part "@" Domain
   * </pre>
   * 
   * @param in The input
   * @return The mailbox
   * @throws ParseException
   */
  protected String readSMTPMailbox(PushbackParserContext in)
    throws ParseException
  {
    StringBuffer mailbox=new StringBuffer();
    mailbox.append(readLocalPart(in));
    in.expect('@');
    mailbox.append('@');
    mailbox.append(Core.parseHostName(in));
    return mailbox.toString();
    
  }

  /**
   * <pre>
   * Local-part = Dot-string / Quoted-string
   * </pre>
   * 
   * @param in
   * @return
   * @throws ParseException
   */
  protected String readLocalPart(PushbackParserContext in)
    throws ParseException
  {
    char chr=(char) in.read();
    in.unread(chr);    
   
    if (Core.isDQUOTE(chr))
    { return InetTextMessages.parseQuotedString(in);
    }
    else if (InetTextMessages.isAtom(chr))
    { return InetTextMessages.parseDotAtomText(in);
    }
    else
    { 
      in.throwParseException("Found unexpected character '"+chr+"'");    
      return null;
    }
  }
  
  
  

  
  protected boolean isLetDig(char chr)
  { return Core.isALPHA(chr) || Core.isDIGIT(chr);
  }
  
  protected boolean isLdhStr(char chr)
  { return isLetDig(chr) || chr=='-';
  }
  
  
  protected PushbackParserContext startParse()
  { 
    PushbackParserContext in=new PushbackParserContext(rawAddress);
    in.getPosition().setContext(rawAddress);
    return in;
  }  
  
  /**
   * <p>Extract up until the specified token or the end of the value is
   *   encountered
   * </p>
   * @param token
   * @return
   */
  protected String extractTokenTo(PushbackParserContext in,char stop)
    throws ParseException
  {
    StringBuilder val=new StringBuilder();
    int c;
    while ((c = in.read()) > -1 && c != stop)
    { val.append((char) c);
    }
    if (c==stop)
    { in.unread((char) c);
    }
    
    return val.toString();
  }
  
}
