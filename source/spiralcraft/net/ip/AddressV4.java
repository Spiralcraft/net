package spiralcraft.net.ip;

import java.util.StringTokenizer;

import spiralcraft.util.ByteBuffer;
import spiralcraft.util.ArrayUtil;

public class AddressV4
{

  /**
   * <p>Parse a dotted quad string into a 4 byte array
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
  
  /**
   * Format an ip address represented as a byte[] to a dotted quad string
   * 
   * @param addr
   * @return
   */
  public static String format(byte[] addr)
  { 
    StringBuilder ret=new StringBuilder();
    for (int i=0;i<addr.length;i++)
    {
      if (i>0) 
      { ret.append('.');
      }
      ret.append(Integer.toString(Byte.toUnsignedInt(addr[i])));
    } 
    return ret.toString();
  
  }
  
  /**
   * Reverse the octed order in a dotted quad string representation of an IP address
   * @param addr
   * @return
   */
  public static String reverse(String addr)
  { return format(ArrayUtil.reverse(parse(addr)));
  }
}