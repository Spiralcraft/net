//
// Copyright (c) 1998,2005 Michael Toth
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
package spiralcraft.codec.base64;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

 
public class Base64Encoder
{
  private static final int BUFSIZE = 1024; 

  private static final byte[] map = 
  {
     (byte) 'A'
    ,(byte) 'B'
    ,(byte) 'C'
    ,(byte) 'D'
    ,(byte) 'E'
    ,(byte) 'F'
    ,(byte) 'G'
    ,(byte) 'H'
    ,(byte) 'I'
    ,(byte) 'J'
    ,(byte) 'K'
    ,(byte) 'L'
    ,(byte) 'M'
    ,(byte) 'N'
    ,(byte) 'O'
    ,(byte) 'P'
    ,(byte) 'Q'
    ,(byte) 'R'
    ,(byte) 'S'
    ,(byte) 'T'
    ,(byte) 'U'
    ,(byte) 'V'
    ,(byte) 'W'
    ,(byte) 'X'
    ,(byte) 'Y'
    ,(byte) 'Z'
    ,(byte) 'a'
    ,(byte) 'b'
    ,(byte) 'c'
    ,(byte) 'd'
    ,(byte) 'e'
    ,(byte) 'f'
    ,(byte) 'g'
    ,(byte) 'h'
    ,(byte) 'i'
    ,(byte) 'j'
    ,(byte) 'k'
    ,(byte) 'l'
    ,(byte) 'm'
    ,(byte) 'n'
    ,(byte) 'o'
    ,(byte) 'p'
    ,(byte) 'q'
    ,(byte) 'r'
    ,(byte) 's'
    ,(byte) 't'
    ,(byte) 'u'
    ,(byte) 'v'
    ,(byte) 'w'
    ,(byte) 'x'
    ,(byte) 'y'
    ,(byte) 'z'
    ,(byte) '0'
    ,(byte) '1'
    ,(byte) '2'
    ,(byte) '3'
    ,(byte) '4'
    ,(byte) '5'
    ,(byte) '6'
    ,(byte) '7'
    ,(byte) '8'
    ,(byte) '9'
    ,(byte) '+'
    ,(byte) '/'
    ,(byte) '='
  };

  private static final int get1(byte buf[], int off) 
  { return (buf[off] & 0xfc) >> 2 ;
  }

  private static final int get2(byte buf[], int off)
  { return ((buf[off]&0x3) << 4) | ((buf[off+1]&0xf0) >>> 4) ;
  }

  private static final int get3(byte buf[], int off)
  { return ((buf[off+1] & 0x0f) << 2) | ((buf[off+2] & 0xc0) >>> 6) ;
  }

  private static final int get4(byte buf[], int off)
  { return buf[off+2] & 0x3f ;
  }

  public static String encodeStringToString(String input)
  { 
    try
    {
      ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      encode(in,out);
      return out.toString();
    }
    catch (IOException x)
    { throw new RuntimeException("Unexpected IOException ",x);
    }
  }
  
  public static void encode(InputStream in,OutputStream out)
    throws IOException
  {
    byte buffer[] = new byte[BUFSIZE] ;
    int  got      = -1 ;
    int  off      = 0 ;
    int  count    = 0 ;
    while ((got = in.read(buffer, off, BUFSIZE-off)) > 0)
    {
      if ( got >= 3 ) 
      {
        got += off;
        off  = 0;
        while (off + 3 <= got) 
        {
          int c1 = get1(buffer,off) ;
          int c2 = get2(buffer,off) ;
          int c3 = get3(buffer,off) ;
          int c4 = get4(buffer,off) ;
          switch (count) 
          {
            case 73:
              out.write(map[c1]);
              out.write(map[c2]);
              out.write(map[c3]);
              out.write ('\n') ;
              out.write(map[c4]) ;
              count = 1 ;
              break ;
            case 74:
              out.write(map[c1]);
              out.write(map[c2]);
              out.write ('\n') ;
              out.write(map[c3]);
              out.write(map[c4]) ;
              count = 2 ;
              break ;
            case 75:
              out.write(map[c1]);
              out.write ('\n') ;
              out.write(map[c2]);
              out.write(map[c3]);
              out.write(map[c4]) ;
              count = 3 ;
              break ;
            case 76:
              out.write('\n') ;
              out.write(map[c1]);
              out.write(map[c2]);
              out.write(map[c3]);
              out.write(map[c4]) ;
              count = 4 ;
              break ;
            default:
              out.write(map[c1]);
              out.write(map[c2]);
              out.write(map[c3]);
              out.write(map[c4]) ;
              count += 4 ;
              break ;
          }
          off += 3 ;
        }

        // Copy remaining bytes to beginning of buffer:
        for ( int i = 0 ; i < 3 ;i++) 
        { buffer[i] = (i < got-off) ? buffer[off+i] : ((byte) 0) ;
        }
        
        off = got-off ;
      } 
      else 
      {
        // Total read amount is less then 3 bytes:
        off += got;
      }

    }

    // Manage the last bytes, from 0 to off:
    switch (off) 
    {
      case 1:
          out.write(map[get1(buffer, 0)]);
          out.write(map[get2(buffer, 0)]);
          out.write('=');
          out.write('=');
          break;
      case 2:
          out.write(map[get1(buffer, 0)]);
          out.write(map[get2(buffer, 0)]);
          out.write(map[get3(buffer, 0)]);
          out.write('=');
    }
    return;
  }

}
