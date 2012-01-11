package spiralcraft.net.mime;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

//import spiralcraft.log.ClassLog;

public class HeaderReader
{
//  private static final ClassLog log
//    =ClassLog.getInstance(HeaderReader.class);
  
  private final PushbackReader in;
  private boolean EOF;
  
  public HeaderReader(String rawValue)
  { in=new PushbackReader(new StringReader(rawValue));
  }

  public int read() 
    throws IOException
  { 
    int read=in.read();
    if (read==-1)
    { EOF=true;
    }
    return read;
  }
  
  public boolean isEOF()
  { return EOF;
  }
  
  public void unread(int c) 
    throws IOException
  { 
    in.unread(c);
    EOF=false;
  }
  
  protected void skipComment()
    throws IOException
  {
    int c;

    while ((c = read()) > -1)
    {
      switch (c)
      {
        case '\\':
          read();
          break;
        case ')':
          return;
      }
    }
    
  }  
  
  
  /**
   * Parses an RFC822 quoted-string. Assumes leading '"' has been consumed
   * from the InputStream.
   *
   * @return    the parsed quoted string - without the quotes and with
   *            escapes properly substituted
   */
  protected String parseQuotedString(PushbackReader in,String quotableChars)
    throws IOException
  {
    StringBuilder qstr = new StringBuilder(20);
    int c;

    while ((c = read()) > -1)
    {
      switch (c)
      {
        case '\\':
          if (quotableChars!=null)
          { 
            char quoted=(char) read();
            if (quotableChars.indexOf(quoted)>=0)
            { qstr.append(quoted);
            }
            else
            { 
              // Pass through backslash if not in quotable chars
              qstr.append("\\").append(quoted);
            }
          }
          else
          { qstr.append((char) read());
          }
          break;
        case '"':
          return qstr.toString();
        default:
          qstr.append((char) c);
      }
    }
    return qstr.toString();
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

    while ((c = read()) > -1)
    {
      switch (c)
      {
        case '\\':
          qstr.append((char) read());
          break;
        case '"':
          return qstr.toString();
        default:
          qstr.append((char) c);
      }
    }
    return qstr.toString();
  }  
  
  protected void skipWhitespace()
    throws IOException
  { 
    int c;

    while ((c = read()) > -1)
    {
      switch (c)
      {
        case ' ':
        case '\t':
          continue;
      }
      unread(c);
      break;
    }
    
  }
  
  protected String parseToken()
    throws IOException
  {
    StringBuilder token = new StringBuilder(20);
    int c;

    while ((c = read()) > -1)
    {
      if (c == ' ')
      {
        if (token.length() == 0)
        { continue; // strip leading spaces
        } 
        else
        {
          unread(c);
          break;
        }
      }
      else if (isControl(c) || isSpecial(c))
      {
        unread(c);
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
  public LinkedHashMap<String,String> extractParameters
    (String quotableChars
    ,boolean stopOnUnknown
    )
    throws IOException
  {
    LinkedHashMap<String,String> parameters=new LinkedHashMap<String,String>();
    int c;

    while ((c = read()) > -1)
    {
      switch (c)
      {
        case ';':
          parseParameter(parameters,quotableChars);
          continue;
        case ' ':
        case '\t':
          continue;
      }
      if (stopOnUnknown)
      { 
        unread(c);
        break;
      }
    }
    return parameters;
  }
  
  public void parseParameter
    (Map<String,String> parameters,String quotableChars)
    throws IOException
  {
    String name = parseToken();
    int c = read();
    if (c == '=')
    {
      c = read();
      if (c == '"')
      { 
        if (quotableChars!=null)
        { parameters.put(name, parseQuotedString(in, quotableChars));
        }
        else
        { parameters.put(name, parseQuotedString(in));
        }
      }
      else
      {
        unread(c);
        parameters.put(name, parseToken());
      }
    }
    else
    {
      unread(c);
      parameters.put(name, "");
    }
  }
  
  
  /**
   * Extract up until the specified token or the end of the value is
   *   encountered, ignoring comments
   * 
   * @param token
   * @return
   */
  public String extractTokenTo(char token)
    throws IOException
  {
    StringBuilder val=new StringBuilder();
    int c;
    while ((c = read()) > -1 && c != token)
    { 
      if (c=='(')
      { skipComment();
      }
      else
      { val.append((char) c);
      }
    }
    if (c==token)
    { unread(c);
    }
    
    return val.toString();
  }  
  
  /**
   * Extract up until the specified tokens or the end of the value is
   *   encountered, ignoring comments
   * 
   * @param token
   * @return
   */
  public String extractTokenTo(String tokens)
    throws IOException
  {
    StringBuilder val=new StringBuilder();
    int c;
    boolean match=false;
    while ((c = read()) > -1)
    { 
      match=tokens.indexOf(c)>-1;
      if (match)
      { break;
      }
      if (c=='(')
      { skipComment();
      }
      else
      { val.append((char) c);
      }
    }
    if (match)
    { unread(c);
    }
    
    return val.toString();
  }  
  
  public boolean isContinued(char separator)
    throws IOException
  { 
    skipWhitespace();
    int c=read();
    if (c==separator)
    { return true;
    }
    else
    { return false;
    }
  }
  
}
