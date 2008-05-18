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
package spiralcraft.net.mime;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.LinkedHashMap;


public abstract class MimeHeader
{
  private String name;
  private String rawValue;
    
  protected MimeHeader(String name,String rawValue)
  {
    this.name=name;
    this.rawValue=rawValue;
  }
  
  public String getName()
  { return name;
  }
  
  /**
   * 
   * @return The unparsed value
   */
  public String getRawValue()
  { return rawValue;
  }

  
  protected PushbackReader startParse()
  { return new PushbackReader(new StringReader(rawValue));
  }

  /**
   * Extract up until the specified token or the end of the value is
   *   encountered, ignoring comments
   * 
   * @param token
   * @return
   */
  protected String extractTokenTo(PushbackReader in,char token)
    throws IOException
  {
    StringBuilder val=new StringBuilder();
    int c;
    while ((c = in.read()) > -1 && c != ';')
    { 
      if (c=='(')
      { skipComment(in);
      }
      else
      { val.append((char) c);
      }
    }
    if (c==';')
    { in.unread(c);
    }
    
    return val.toString();
  }
  
  protected void skipComment(PushbackReader in)
    throws IOException
  {
    int c;

    while ((c = in.read()) > -1)
    {
      switch (c)
      {
        case '\\':
          in.read();
          break;
        case ')':
          return;
      }
    }
    
  }
   
  /**
   * Extract parameters in the form 
   *   
   *   parameters := attrVal *[";" attrVal]
   *   
   *   attrVal := token "=" token/quotedString
   *   
   * @param in
   * @return
   * @throws IOException
   */
  protected LinkedHashMap<String,String> extractParameters(PushbackReader in)
    throws IOException
  {
    LinkedHashMap<String,String> parameters=new LinkedHashMap<String,String>();
    String name;
    int c;

    while ((c = in.read()) > -1)
    {
      switch (c)
      {
        case ';':
          name = parseToken(in);
          c = in.read();
          if (c == '=')
          {
            c = in.read();
            if (c == '"')
            { parameters.put(name, parseQuotedString(in));
            }
            else
            {
               in.unread(c);
              parameters.put(name, parseToken(in));
            }
          }
          else
          {
            in.unread(c);
            parameters.put(name, "");
          }
          break;
        default:
          /* ignore */
      }
    }
    return parameters;
  }
  
  /**
   * Parses an RFC822 quoted-string. Assumes leading '"' has been consumed
   * from the InputStream.
   *
   * @return    the parsed quoted string - without the quotes and with
   *            escapes properly substituted
   */
  protected String parseQuotedString(PushbackReader in)
    throws IOException
  {
    StringBuilder qstr = new StringBuilder(20);
    int c;

    while ((c = in.read()) > -1)
    {
      switch (c)
      {
        case '\\':
          qstr.append((char) in.read());
          break;
        case '"':
          return qstr.toString();
        default:
          qstr.append((char) c);
      }
    }
    return qstr.toString();
  }
  
  protected String parseToken(PushbackReader in)
    throws IOException
  {
    StringBuilder token = new StringBuilder(20);
    int c;

    while ((c = in.read()) > -1)
    {
      if (c == ' ')
      {
        if (token.length() == 0)
        { continue; // strip leading spaces
        } 
        else
        {
          in.unread(c);
          break;
        }
      }
      else if (isControl(c) || isSpecial(c))
      {
        in.unread(c);
        break;
      }
      token.append((char) c);
    }
    return token.toString();
  }

  private boolean isControl(int c)
  { return c>=0 && c<32;
  }

  private boolean isSpecial(int c)
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
      case '"':
      case '/':
      case '[':
      case ']':
      case '?':
      case '=':
        return true;
      default:
        return false;
    }
  }
  
}
