//
// Copyright (c) 1998,2007 Michael Toth
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

import java.io.InputStream;
import java.io.IOException;

import spiralcraft.util.BytePatternMatcher;

/**
 * <P>Separates a stream around occurrences of a byte pattern.
 * </P>
 * 
 * <P>This InputStream deviates from the standard design contract insomuch as 
 *   whenever the  part delimiter (byte pattern) is encountered, read returns
 *   -1, which should cue the user to start reading the next part. If another
 *   -1 is returned immediately, the end of the stream (or the contentLength)
 *   has been reached.
 * </P> 
 */
public class MultipartInputStream
  extends InputStream
{

  private final BytePatternMatcher detector;
  private final int contentLength;
  private final InputStream in;
  private boolean lastWasMatch=false;
  private int count;

  public MultipartInputStream(InputStream in,byte[] separator,int contentLength)
  { 
    detector=new BytePatternMatcher(separator);
    this.contentLength=contentLength;
    this.in=in;
  }

  public final int read(byte[] buf,int start,int len)
    throws IOException
  {
    if (lastWasMatch)
    { 
      lastWasMatch=false;
      return -1;
    }

    if (count>=contentLength)
    { return -1;
    }

    for (int i=0;i<len && count++<contentLength;i++)
    {
      final int next=in.read();
      if (next==-1)
      { 
        if (i==0)
        { return -1;
        }
        else
        { return i;
        }
      }
      final byte b=(byte) next;
      buf[i+start]=b;
      lastWasMatch=detector.match(b);
      if (lastWasMatch)
      { return i+1;
      }
    }
    return len;

  }

  public final int read(byte[] buf)
    throws IOException
  { return read(buf,0,buf.length);
  }

  public final int read()
    throws IOException
  { 
    byte[] b=new byte[1];
    int val=read(b,0,1);
    if (val==-1)
    { return val;
    }
    else
    {
      // Remove sign
      return b[0] & 0xFF;
    }
  }


  public void close()
    throws IOException
  { in.close();
  }
}
