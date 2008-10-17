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

import spiralcraft.text.ParseException;
import spiralcraft.text.PushbackParserContext;

/**
 * <p>Provides validation and parsing support for common syntactic elements in
 *   RFC defined protocols, primarily the "core" syntax referred to in 
 *   RFC-2234 (6. Appendix A).
 * </p>
 * 
 * @author mike
 *
 */
public class Core
{

  /**
   * @param c
   * @return Whether c is in A-Z/a-z
   */
  public static final boolean isALPHA(char c)
  { return (c>='A' && c<='Z') || (c>='a' && c<='z');
  }
  
  /**
   * @param c
   * @return Whether c is in "0"/"1"
   * 
   */
  public static final boolean isBIT(char c)
  { return c=='1' || c=='0';
  }

  /**
   * @param c
   * @return Whether c is in %x01-7F
   * 
   */
  public static final boolean isCHAR(char c)
  { return c>0 && c<=0x7F;
  }  
  
  public static final boolean isCR(char c)
  { return c=='\r';
  }
  
  public static final boolean isLF(char c)
  { return c=='\n';
  }  

  public static final boolean isCTL(char c)
  { return c<=0x1F || c==0x7F;
  }  
  
  public static final boolean isDIGIT(char c)
  { return c>=0x30 && c<=0x39;
  }
  
  public static final boolean isDQUOTE(char c)
  { return c=='"';
  }
  
  public static final boolean isHEXDIG(char c)
  { return isDIGIT(c) || (c>='A' && c<='F');
  }
  
  public static final boolean isHTAB(char c)
  { return c=='\t';
  }
  
  public static final boolean isOCTET(char c)
  { return c>=0 && c<=0xFF;
  }
  
  public static final boolean isSP(char c)
  { return c==' ';
  }
  
  public static final boolean isVCHAR(char c)
  { return c>=0x21 && c<=0x7E;
  }
  
  /**
   * 
   * @param c
   * @return Whether c is a whitespace char
   */
  public static final boolean isWSP(char c)
  { return isSP(c) || isHTAB(c);
  }
  
  /**
   * <p>Parse a host name (RFC 1123). Host name
   *   components are separated from each other by the "." character.
   * </p> 
   * 
   * <p>This method will return the parsed host name and unread the first
   *   symbol following the name
   * </p>
   * 
   * @param context
   * @return The domain name component
   */    
  public static final String parseHostName(PushbackParserContext in)
    throws ParseException
  { 
    StringBuffer hostname=new StringBuffer();
    while (true)
    {
      hostname.append(parseHostNameComponent(in));
      if (in.isEOF())
      { break;
      }
      char chr=(char) in.read();
      if (chr=='.')
      { hostname.append('.');
      }
      else
      { 
        if (!in.isEOF())
        { in.unread(chr);
        }
        break;
      }
      
    }
    return hostname.toString();
  }
  
  /**
   * <p>Parse a host name component (RFC 1123) from within a host name.
   *   Host name
   *   components are separated from each other by the "." character.
   * </p> 
   * 
   * <p>This method will return the parsed host name component and unread the
   *   next symbol not part of the host name component
   * </p>
   * 
   * @param context
   * @return The domain name component
   */
  public static final String parseHostNameComponent(PushbackParserContext in)
    throws ParseException
  {
    char chr=(char) in.read();
    StringBuffer subname=new StringBuffer();
    if (!isALPHAorDIGIT(chr))
    { 
      in.throwParseException
        ("Host name component must begin with a letter or digit, not '"+chr+"'");
    }
    subname.append(chr);
    while (true)
    {
      chr=(char) in.read();

      if (isHostNameComponentInternal(chr))
      { subname.append(chr);
      }
      else
      { 
        in.unread(chr);
        break;
      }
      
    }
    if (!isALPHAorDIGIT(subname.charAt(subname.length()-1)))
    { 
      in.throwParseException
        ("Domain name component must end in a letter or a digit");
    }
    return subname.toString();
  }
  
  protected static final boolean isALPHAorDIGIT(char chr)
  { return Core.isALPHA(chr) || Core.isDIGIT(chr);
  }
  
  protected static final boolean isHostNameComponentInternal(char chr)
  { return isALPHAorDIGIT(chr) || chr=='-';
  }
  
}
