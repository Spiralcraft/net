package spiralcraft.http;

import spiralcraft.util.ArrayMap;

import java.util.LinkedHashMap;

/**
 * Encodes and decodes urlencoded form and query variables
 */
public class VariableMap
  extends ArrayMap
{

  public static final VariableMap fromUrlEncodedString(String encodedForm)
  {
    VariableMap map=new VariableMap();
    map.parseEncodedForm(encodedForm);
    return map;
  }

  public VariableMap()
  { super(new LinkedHashMap(),String.class);
  }

  public void parseEncodedForm(String encodedForm)
  {
  }
}
