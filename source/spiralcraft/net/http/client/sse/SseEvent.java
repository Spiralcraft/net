package spiralcraft.net.http.client.sse;

import java.util.List;

public record SseEvent(
  Kind kind
  ,String name
  ,String data
  ,String id
  ,int sequence
  ,int retryMs
  ,String text
  ,List<SseLine> lines
  )
{
  public enum Kind { EVENT, COMMENT, END }

  /** The type of an event when none is sent, per the WHATWG standard */
  public static final String DEFAULT_TYPE="message";

  /**
   * The type of an EVENT as the standard dispatches it: the name, or
   *   "message" when none was sent. Null for a COMMENT or END item.
   */
  public String type()
  { 
    if (kind!=Kind.EVENT)
    { return null;
    }
    return name!=null?name:DEFAULT_TYPE;
  }
}