package spiralcraft.net.mime;

import java.io.IOException;

import spiralcraft.util.string.StringConverter;

public class MimeHeaderToString
  extends StringConverter<MimeHeader>
{


  @Override
  public MimeHeader fromString(
    String val)
  { 
    try
    { return MimeHeader.parse(val, null);
    }
    catch (IOException x)
    { throw new IllegalArgumentException("Error parsing "+val);
    }
  }
  
  public String toString(MimeHeader val)
  { return val==null?null:val.getRawValue();
  }
  
}