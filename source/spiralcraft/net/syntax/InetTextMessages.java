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

import java.util.Date;

import spiralcraft.text.ParseException;
import spiralcraft.text.PushbackParserContext;
import spiralcraft.util.string.DateToString;
import spiralcraft.util.string.StringConverter;

/**
 * <p>Provides validation and parsing support for common syntactic elements in
 *   RFC 822 and successors: "Format Of ARPA Internet Text Messages"
 * </p>
 * 
 * @author mike
 *
 */
public class InetTextMessages
{

  private static final StringConverter<Date> RFC822_DATE_CONVERTER
    =new DateToString("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z");
  
  /**
   * <pre>
   * specials    =  "(" / ")" / "&lt;" / "&gt;" / "@"  ; Must be in quoted-
   *             /  "," / ";" / ":" / "\" / &lt;"&gt;  ;  string, to use
   *             /  "." / "[" / "]"                    ;  within a word.
   * </pre>
   * 
   * 
   * @param c
   * @return Whether c is  a special char
   */
  public static final boolean isSpecial(char c)
  { 
    switch (c)
    {
    case '(':
    case ')':
    case '<':
    case '>':
    case '@':
    case ',':
    case ';':
    case ':':
    case '\\':
    case '\"':
    case '.':
    case '[':
    case ']':
      return true;
    default:
      return false;
    }
  }
  
  public static final boolean isLWSP(char c)
  { return Core.isSP(c) || Core.isHTAB(c);
  }
      
  /**
   * <pre>
   * atom        =  1*&lt;any CHAR except specials, SPACE and CTLs&gt;
   * </pre>
   *
   * @param c
   * @return Whether c is a valid atom character
   */    
  public static final boolean isAtom(char c)
  {
    return Core.isCHAR(c) 
      && !Core.isCTL(c) 
      && !isSpecial(c) 
      && !Core.isSP(c);
  }
  
  /**
   * <pre>
   * qtext       =  &lt;any CHAR excepting &lt;"&gt;,     ; =&gt; may be folded
   *                 \" & CR, and including
   *                 linear-white-space&gt;
   * </pre>
   *
   * @param c
   * @return Whether c is a valid atom character
   */    
  public static final boolean isQtext(char c)
  {
    return Core.isCHAR(c) 
      && !Core.isDQUOTE(c) 
      && !Core.isCR(c) 
      && c!='\\';
  }
  
  /**
   * <pre>
   * atom        =  1*&lt;any CHAR except specials, SPACE and CTLs&gt;
   * </pre>
   *
   * @param context
   * @return The atom starting at the current position.
   */    
  public static final String parseAtom(PushbackParserContext in)
    throws ParseException
  { 
    StringBuffer atom=new StringBuffer();
    while (true)
    {
      char chr=(char) in.read();
      if (isAtom(chr))
      { atom.append(chr);
      }
      else
      { 
        in.unread(chr);
        break;
      }
    }
    return atom.toString();
  }

  /**
   * <pre>
   * quoted-string = &lt;"&gt; *(qtext/quoted-pair) &lt;"&gt; ;Regular qtext or
   *                                              ;   quoted chars.
   * </pre>
   * 
   * @param in
   * @return
   * @throws ParseException
   */
  public static final String parseQuotedString(PushbackParserContext in)
    throws ParseException
  {
    StringBuffer text=new StringBuffer();
    in.expect('"');
    text.append('"');
    while (true)
    {
      int ret=in.read();
      if (ret==-1)
      { in.throwParseException("Unexpected end of input");
      }
      
      char chr=(char) ret;
      if (chr=='\\')
      {
        // Quoted pair
        text.append('\\');
        text.append((char) in.read());
      }
      else if (chr=='"')
      { 
        text.append('"');
        break;
      }
      else if (isQtext(chr))
      { text.append(chr);
      }
      else
      { in.throwParseException("Illegal character '"+chr+"' in quoted string");
      }
    }
    return text.toString();
  }
  
  /**
   * <pre>
   *   dot-atom-text   =   1*atext *("." 1*atext)
   * </pre>
   * 
   * @param in
   * @return
   * @throws ParseException
   */
  public static final String parseDotAtomText(PushbackParserContext in)
    throws ParseException
  {
    StringBuffer out=new StringBuffer();
    while (true)
    {
      char chr=(char) in.read();

      if (isAtom(chr))
      { 
        in.unread(chr);
        out.append(parseAtom(in));
      }
      else if (chr=='.')
      { 
        out.append(chr);
      }
      else
      {
        if (!in.isEOF())
        { in.unread(chr);
        }
        break;
      }
      
    }
    return out.toString();
  }  
  
  /**
   * <pre>
   * qtext       =  &lt;any CHAR excepting &lt;"&gt;,     ; =&gt; may be folded
   *                 \" & CR, and including
   *                 linear-white-space&gt;
   * </pre>
   * 
   * @param in
   * @return
   * @throws ParseException
   */
  public static final String parseQtext(PushbackParserContext in)
    throws ParseException
  {
    StringBuffer qtext=new StringBuffer();
    while (true)
    {
      char chr=(char) in.read();
      if (isQtext(chr))
      { qtext.append(chr);
      }
      else
      { 
        in.unread(chr);
        break;
      }
    }
    return qtext.toString();
  }

  /**
   * <pre>
   * word        =  atom / quoted-string
   * </pre>
   * 
   * @param in
   * @return The word
   * @throws ParseException
   */
  protected static final String parseWord(PushbackParserContext in)
    throws ParseException
  {
    char chr=(char) in.read();
    in.unread(chr);    
   
    if (Core.isDQUOTE(chr))
    { return parseQuotedString(in);
    }
    else if (isAtom(chr))
    { return parseAtom(in);
    }
    else
    { return null;
    }
  }
  
  protected static final boolean isWord(char chr)
  { return Core.isDQUOTE(chr) || isAtom(chr);
  }
  
  /**
   * <pre>
   * phrase = 1*word 
   * </pre>
   * 
   * @param in
   * @return The phrase
   * @throws ParseException
   */
  public static final String parsePhrase(PushbackParserContext in)
    throws ParseException
  {
   
    StringBuffer phrase=new StringBuffer();
    while (true)
    {
      char chr=(char) in.read();
      in.unread(chr);    
      if (isWord(chr))
      { phrase.append(parseWord(in));
      }
      else if (isLWSP(chr))
      { 
        parseLWSP(in);
        if (phrase.length()>0)
        { phrase.append(" ");
        }
      }
      else
      { break;
      }
    }
    return phrase.toString().trim();
  }
  
  /**
   * <pre>
   * atom        =  1*&lt;any CHAR except specials, SPACE and CTLs&gt;
   * </pre>
   *
   * @param context
   * @return The atom starting at the current position.
   */    
  public static final String parseLWSP(PushbackParserContext in)
    throws ParseException
  { 
    StringBuffer lwsp=new StringBuffer();
    while (true)
    {
      char chr=(char) in.read();
      if (isLWSP(chr))
      { lwsp.append(chr);
      }
      else
      { 
        in.unread(chr);
        break;
      }
    }
    return lwsp.toString();
  }  
  
  public static final StringConverter<Date> dateConverter()
  { return RFC822_DATE_CONVERTER;
  }
  
}
