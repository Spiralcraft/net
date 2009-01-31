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


import java.util.List;
import java.util.ArrayList;

import java.util.Iterator;

import spiralcraft.util.ArrayUtil;

/**
 * <p>Determines whether an IP address is contained within a set of IP 
 *   address ranges indicated by a set of Subnets
 * </p>
 * 
 * @author mike
 */
public class AddressSet
{

  private List<Subnet> list=new ArrayList<Subnet>();
  
  /**
   * Construct an empty AddressSet
   */
  public AddressSet()
  {
  }
  
  /**
   * Construct an AddressSet which consists of the subnets specified in a 
   *   comma delimited list 
   */
  public AddressSet(String subnetList)
  { this(subnetList.split(","));
  }
  
  /**
   * Construct an AddressSet which consists of the subnets specified in an
   *   array of Strings.
   *   
   * @param subnets
   */
  public AddressSet(String[] subnets)
  { 
    for (String subnet: subnets)
    { list.add(new Subnet(subnet));
    }
  }
  
  /**
   * @param name
   */
  public void add(Subnet subnet)
  { list.add(subnet);
  }

  public void setContents(Subnet[] subnets)
  { 
    list.clear();
    if (subnets!=null)
    {
      for (Subnet net:subnets)
      { list.add(net);
      }
    }
  
  }
  
  /**
   * <p>Indicate whether this AddressSet contains the specified IP address
   *   represented as a String.
   * </p>
   * 
   * @param address
   * @return Whether any of the subnets contain the IP address
   */
  public boolean contains(String address)
  { return contains(Subnet.parse(address));
  }
      
  /**
   * <p>Indicate whether this AddressSet contains the specified IP address
   *   represented as a sequence of bytes.
   * </p>
   * 
   * @param address
   * @return Whether any of the subnets contain the IP address
   */
  public boolean contains(byte[] address)
  { 
    Iterator<Subnet> it=list.iterator();
    while (it.hasNext())
    { 
      Subnet subnet=it.next();
      if (subnet.contains(address))
      { return true;
      }
    }
    return false;
    
  }

  @Override
  public String toString()
  { return ArrayUtil.format(list.toArray(),",",null);
  }
}
