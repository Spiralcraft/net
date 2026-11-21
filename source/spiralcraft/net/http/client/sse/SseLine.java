package spiralcraft.net.http.client.sse;


public record SseLine(Kind kind,String field,String value)
{
  public enum Kind { FIELD, COMMENT, BLANK, PARTIAL, END }

}
