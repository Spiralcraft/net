//
// Copyright (c) 1998,2009 Michael Toth
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
package spiralcraft.net.ip;

import spiralcraft.util.ByteBuffer;

import java.util.StringTokenizer;

/**
 * <p>An IP address range indicated by a network address and a mask
 * </p>
 * @author mike
 *
 */
public class Subnet
{

  /**
   * <p>Parse a netmask specification
   * </p>
   * 
   * <p>If the specification is in the form a.b.c.d, the mask will be parsed
   *   like an IP address. If the specification is a single number, it will
   *   be interpreted as the number of 1's in the mask.
   * </p>
   * 
   * 
   * @param mask
   * @return
   */
  public static byte[] parseMask(String mask)
  {
    if (mask.contains("."))
    { return parse(mask);
    }
    else
    {
      int num=Integer.parseInt(mask);
      int full=num/8;
      byte[] byteMask=new byte[full+1];
      for (int i=0;i<full;i++)
      { byteMask[i]=(byte) 255;
      }
      byteMask[full]= (byte) (255 << (8-(num%8)));
      return byteMask;
    }
  }
  
  /**
   * <p>Parse a string into a raw IP address
   * </p>
   * 
   * @param addr
   * @return
   */
  public static byte[] parse(String addr)
  {
    ByteBuffer buf=new ByteBuffer();
    StringTokenizer tok=new StringTokenizer(addr,".");
    while (tok.hasMoreTokens())
    { buf.append((byte) Integer.parseInt(tok.nextToken()));
    }
    return buf.toByteArray();
  }  
  
  private byte[] network;
  private byte[] netmask;

  
  public Subnet(String address)
  {
    int slashPos=address.indexOf('/');

    if (slashPos>-1)
    { 
      network=parse(address.substring(0,slashPos));
      netmask=parseMask(address.substring(slashPos+1));
    }
    else
    { network=parse(address);
    }
    if (netmask!=null)
    {
      for (int i=0;i<netmask.length;i++)
      { network[i]=(byte) (network[i] & netmask[i]);
      }
      if (network.length>netmask.length)
      { 
        for (int i=netmask.length;i<network.length;i++)
        { network[i]=0;
        }
      }
    }

  }

  public boolean contains(byte[] address)
  {
    if (netmask!=null)
    {
      for (int i=0;i<netmask.length;i++)
      { 
        if ((address[i] & netmask[i]) != network[i])
        { return false;
        }
      }
      return true;
    }
    else
    {
      for (int i=0;i<network.length;i++)
      { 
        if (address[i] != network[i])
        { return false;
        }
      }
      return true;
    }
  }


  
  @Override
  public String toString()
  {
    if (netmask==null)
    { return toString(network);
    }
    else
    { 
      return toString(network)
        +"/"+toString(netmask);
    }
  }
  
  public String toString(byte[] address)
  {
    StringBuilder buf=new StringBuilder();
    boolean first=true;
    for (byte b: address)
    { 
      if (first)
      { first=false;
      }
      else
      { buf.append('.');
      }
      buf.append(b & 255);
    }
    return buf.toString();
  }

}
