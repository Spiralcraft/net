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
  public enum Kind { EVENT, KEEP_ALIVE, END }
}
