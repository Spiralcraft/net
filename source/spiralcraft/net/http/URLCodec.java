//
//Copyright (c) 1998,2007 Michael Toth
//Spiralcraft Inc., All Rights Reserved
//
//This package is part of the Spiralcraft project and is licensed under
//a multiple-license framework.
//
//You may not use this file except in compliance with the terms found in the
//SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
//at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
//Unless otherwise agreed to in writing, this software is distributed on an
//"AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net.http;


/**
 * Encodes and decodes URLEncoded text
 * 
 * @author mike
 *
 */
public class URLCodec
{  
  public static String encode(String plaintext)
  {
    StringBuilder encoded = new StringBuilder();
    char[] chars = plaintext.toCharArray();
    for (int i=0; i<chars.length; i++)
    {
      char c = chars[i];
      if (c==' ')
      { encoded.append('+');
      }
      else if ( ! ((c>='A' && c<='Z') || 
              (c>='a' && c<='z') || 
              (c>='0' && c<='9')))
      {
        encoded.append('%');
        String hex=(Integer.toHexString(c));
        if (hex.length()==1)       
        { encoded.append("0");
        }
        encoded.append(hex);
      }
      else
      { encoded.append(c);
      }

    }
    return encoded.toString();
  }
  
  /**
   * Decode a URLEncoded string. Return null if
   *   there was a format problem.
   */
  public static String decode(String encodedText)
  {
    // System.out.println("encoded Text: "+encodedText);
    StringBuilder decoded = new StringBuilder();
    char[] chars = encodedText.toCharArray();
    for (int i=0; i<chars.length; i++)
    {
      char c = chars[i];
      if (c == '+')
      { decoded.append(' ');
      }
      else if (c == '%')
      {
        try
        {
          // convert hexvalue to character
          char[] hex={chars[++i],chars[++i]};
          decoded.append( (char) Integer.valueOf(new String(hex), 16).intValue());                  
        }
        catch (Exception x)
        { return null;
        }
        
      }
      else
      {
        decoded.append(c);
      }
    }
    //System.out.println("plaintext: "+decoded.toString());
    return decoded.toString();
  }

}