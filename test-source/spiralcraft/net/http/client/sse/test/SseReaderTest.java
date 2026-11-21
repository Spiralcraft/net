package spiralcraft.net.http.client.sse.test;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import spiralcraft.net.http.client.sse.SseEvent;
import spiralcraft.net.http.client.sse.SseEvent.Kind;
import spiralcraft.net.http.client.sse.SseLine;
import spiralcraft.net.http.client.sse.SseLineReader;
import spiralcraft.net.http.client.sse.SseReader;

/**
 * Standalone checks of SseLineReader and SseReader: java.io only.
 */
public class SseReaderTest
{
  static int checks;
  static int failures;

  /** Delivers at most chunk bytes per read; fails if read past the end */
  static class Stream
    extends InputStream
  {
    final byte[] bytes;
    final int chunk;
    final boolean strict;
    int at;
    boolean closed;

    Stream(byte[] bytes,int chunk,boolean strict)
    { this.bytes=bytes; this.chunk=chunk; this.strict=strict;
    }

    @Override
    public int read()
    { throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b,int off,int len)
    {
      if (at==bytes.length)
      { 
        if (strict)
        { throw new IllegalStateException("read past the end");
        }
        return -1;
      }
      int n=Math.min(Math.min(len,chunk),bytes.length-at);
      System.arraycopy(bytes,at,b,off,n);
      at+=n;
      return n;
    }

    @Override
    public void close()
    { closed=true;
    }
  }

  static byte[] utf8(String s)
  { return s.getBytes(StandardCharsets.UTF_8);
  }

  static Stream whole(String s)
  { return new Stream(utf8(s),8192,false);
  }

  static Stream trickle(String s)
  { return new Stream(utf8(s),1,false);
  }

  /** Fails if the reader reads again after the last byte */
  static Stream strictWhole(String s)
  { return new Stream(utf8(s),8192,true);
  }

  static Stream strictTrickle(String s)
  { return new Stream(utf8(s),1,true);
  }

  /** Failure messages of the group being run */
  static final List<String> messages=new ArrayList<>();

  static void eq(String what,Object expected,Object actual)
  {
    checks++;
    if (!expected.equals(actual))
    { 
      failures++;
      messages.add
        (what+": expected ["+show(expected)+"] actual ["+show(actual)+"]");
    }
  }

  /** The value on one line, with control characters made visible */
  static String show(Object o)
  { 
    return String.valueOf(o)
      .replace("\n","\\n").replace("\r","\\r").replace("\0","\\0");
  }

  static SseEvent ev(String name,String data,String id,int seq,int retry)
  { 
    return new SseEvent
      (Kind.EVENT,name,data,id,seq,retry,null,List.of());
  }

  static SseEvent ka(String text)
  { return new SseEvent(Kind.KEEP_ALIVE,null,null,null,0,-1,text,List.of());
  }

  static final SseEvent END
    =new SseEvent(Kind.END,null,null,null,0,-1,null,List.of());

  static SseLine f(String field,String value)
  { return new SseLine(SseLine.Kind.FIELD,field,value);
  }

  /** Every item up to and including the first END */
  static List<SseEvent> all(InputStream in,boolean keep)
    throws IOException
  {
    try (SseReader r=new SseReader(in,keep))
    {
      List<SseEvent> out=new ArrayList<>();
      while (true)
      {
        SseEvent e=r.next();
        out.add(e);
        if (e.kind()==Kind.END)
        { return out;
        }
      }
    }
  }

  static List<SseLine> lines(InputStream in)
    throws IOException
  {
    try (SseLineReader r=new SseLineReader(in))
    {
      List<SseLine> out=new ArrayList<>();
      while (true)
      {
        SseLine l=r.next();
        out.add(l);
        if (l.kind()==SseLine.Kind.END)
        { return out;
        }
      }
    }
  }

  static List<SseEvent> events(String s)
    throws IOException
  { return all(whole(s),false);
  }

  static final String[] GROUPS=
    { "lineForms","lineEndings","bomAndUtf8","fieldsAndGrouping","names"
    , "idAndSequence","retry","commentsAndKeepLines","endOfStream","lineCap"
    , "noReadAfterDispatch","failuresAndClose"
    };

  /**
   * Runs one group of checks and returns its failure messages, empty if
   *   they all passed. Called by the test assembly (SseTest.assy.xml).
   */
  public static List<String> group(String name)
    throws Exception
  {
    messages.clear();
    switch (name)
    {
      case "lineForms": lineForms(); break;
      case "lineEndings": lineEndings(); break;
      case "bomAndUtf8": bomAndUtf8(); break;
      case "fieldsAndGrouping": fieldsAndGrouping(); break;
      case "names": names(); break;
      case "idAndSequence": idAndSequence(); break;
      case "retry": retry(); break;
      case "commentsAndKeepLines": commentsAndKeepLines(); break;
      case "endOfStream": endOfStream(); break;
      case "lineCap": lineCap(); break;
      case "noReadAfterDispatch": noReadAfterDispatch(); break;
      case "failuresAndClose": failuresAndClose(); break;
      default: throw new IllegalArgumentException("No such group: "+name);
    }
    return new ArrayList<>(messages);
  }

  public static void main(String[] args)
    throws Exception
  {
    for (String name : GROUPS)
    { 
      for (String message : group(name))
      { System.out.println("FAIL "+name+": "+message);
      }
    }
    System.out.println(checks+" checks, "+failures+" failures");
    System.exit(failures==0?0:1);
  }

  static void lineForms()
    throws Exception
  {
    List<SseLine> l=lines(whole
      ("data:x\ndata: x\ndata:  x\ndata\ndata:\n: ping\n:ping\n:\na:b:c\n\n"));
    eq("line data:x",f("data","x"),l.get(0));
    eq("line one space",f("data","x"),l.get(1));
    eq("line two spaces",f("data"," x"),l.get(2));
    eq("line no colon",f("data",""),l.get(3));
    eq("line empty value",f("data",""),l.get(4));
    eq("comment",new SseLine(SseLine.Kind.COMMENT,null,"ping"),l.get(5));
    eq("comment no space"
      ,new SseLine(SseLine.Kind.COMMENT,null,"ping"),l.get(6));
    eq("empty comment",new SseLine(SseLine.Kind.COMMENT,null,""),l.get(7));
    eq("second colon",f("a","b:c"),l.get(8));
    eq("blank",new SseLine(SseLine.Kind.BLANK,null,null),l.get(9));
    eq("end",new SseLine(SseLine.Kind.END,null,null),l.get(10));
    eq("line count",11,l.size());

    try (SseLineReader r=new SseLineReader(whole("")))
    {
      SseLine end=r.next();
      eq("empty stream",SseLine.Kind.END,end.kind());
      eq("end repeats",end,r.next());
    }

    eq("default cap",16*1024*1024,SseLineReader.DEFAULT_MAX_LINE_BYTES);
  }

  static void lineEndings()
    throws Exception
  {
    String s="data: a\n\ndata: b\r\n\r\ndata: c\r\r";
    List<SseEvent> want=List.of
      (ev(null,"a",null,0,-1),ev(null,"b",null,0,-1)
      ,ev(null,"c",null,0,-1),END
      );
    eq("endings whole",want,all(whole(s),false));
    eq("endings one byte",want,all(trickle(s),false));
    List<SseLine> crlf=List.of
      (f("a","b"),new SseLine(SseLine.Kind.BLANK,null,null)
      ,f("c","d"),new SseLine(SseLine.Kind.BLANK,null,null)
      ,new SseLine(SseLine.Kind.END,null,null)
      );
    eq("CRLF lines whole",crlf,lines(whole("a:b\r\n\r\nc:d\r\n\r\n")));
    eq("CRLF lines one byte",crlf,lines(trickle("a:b\r\n\r\nc:d\r\n\r\n")));
    eq("CR lines",crlf,lines(whole("a:b\r\rc:d\r\r")));
    eq("CR then LF split"
      ,List.of(ev(null,"a",null,0,-1),END)
      ,all(trickle("data: a\r\n\r\n"),false)
      );
  }

  static void bomAndUtf8()
    throws Exception
  {
    byte[] bom={(byte) 0xEF,(byte) 0xBB,(byte) 0xBF};
    byte[] body=utf8("data: x\n\n");
    byte[] all=new byte[bom.length+body.length];
    System.arraycopy(bom,0,all,0,3);
    System.arraycopy(body,0,all,3,body.length);
    List<SseEvent> want=List.of(ev(null,"x",null,0,-1),END);
    eq("bom whole",want,all(new Stream(all,8192,false),false));
    eq("bom one byte",want,all(new Stream(all,1,false),false));
    eq("bom only",List.of(END),all(new Stream(bom,1,false),false));

    List<SseLine> l=lines(whole("a:b\n\uFEFFc:d\n"));
    eq("bom later is kept",f("\uFEFFc","d"),l.get(1));

    String s="data: \u00e9\u20ac\uD83D\uDE00\n\n";
    eq("utf8 split across reads"
      ,List.of(ev(null,"\u00e9\u20ac\uD83D\uDE00",null,0,-1),END)
      ,all(trickle(s),false)
      );
  }

  static void fieldsAndGrouping()
    throws Exception
  {
    eq("data joined"
      ,List.of(ev(null,"a\nb",null,0,-1),END)
      ,events("data: a\ndata: b\n\n")
      );
    eq("one empty data line is an event with empty data"
      ,List.of(ev(null,"",null,0,-1),END)
      ,events("data\n\n")
      );
    eq("two empty data lines"
      ,List.of(ev(null,"\n",null,0,-1),END)
      ,events("data:\ndata:\n\n")
      );
    eq("blank lines alone give no event"
      ,List.of(END)
      ,events("\n\n\n")
      );
    eq("fields without data give no event and are reset"
      ,List.of(ev(null,"y",null,0,-1),END)
      ,events("event: x\n\ndata: y\n\n")
      );
    eq("unknown field ignored"
      ,List.of(ev(null,"1",null,0,-1),END)
      ,events("foo: bar\ndata: 1\n\n")
      );
  }

  static void names()
    throws Exception
  {
    eq("names"
      ,List.of
        (ev("foo","1",null,0,-1)
        ,ev(null,"2",null,0,-1)
        ,ev("b","3",null,0,-1)
        ,ev(null,"4",null,0,-1)
        ,ev(null,"5",null,0,-1)
        ,END
        )
      ,events
        ("event: foo\ndata: 1\n\ndata: 2\n\n"
         +"event: a\nevent: b\ndata: 3\n\n"
         +"event:\ndata: 4\n\n"
         +"event\ndata: 5\n\n"
        )
      );
  }

  static void idAndSequence()
    throws Exception
  {
    eq("id and sequence"
      ,List.of
        (ev(null,"0",null,0,-1)
        ,ev(null,"1","a",0,-1)
        ,ev(null,"2","a",1,-1)
        ,ev(null,"3","a",2,-1)
        ,ev(null,"4","b",0,-1)
        ,END
        )
      ,events
        ("data: 0\n\nid: a\ndata: 1\n\ndata: 2\n\ndata: 3\n\n"
         +"id: b\ndata: 4\n\n"
        )
      );
    eq("same id again starts again"
      ,List.of(ev(null,"1","a",0,-1),ev(null,"2","a",0,-1),END)
      ,events("id: a\ndata: 1\n\nid: a\ndata: 2\n\n")
      );
    eq("id with NUL ignored"
      ,List.of(ev(null,"1",null,0,-1),END)
      ,events("id: a\u0000b\ndata: 1\n\n")
      );
    eq("empty id"
      ,List.of(ev(null,"1","",0,-1),END)
      ,events("id:\ndata: 1\n\n")
      );
    eq("id set by an event without data"
      ,List.of(ev(null,"q","z",0,-1),END)
      ,events("id: z\n\ndata: q\n\n")
      );
    eq("id carried across a comment"
      ,List.of(ev(null,"1","a",0,-1),ka("c"),ev(null,"2","a",1,-1),END)
      ,events("id: a\ndata: 1\n\n: c\ndata: 2\n\n")
      );
  }

  static void retry()
    throws Exception
  {
    eq("retry carried"
      ,List.of
        (ev(null,"0",null,0,-1)
        ,ev(null,"1",null,0,3000)
        ,ev(null,"2",null,0,3000)
        ,END
        )
      ,events("data: 0\n\nretry: 3000\ndata: 1\n\ndata: 2\n\n")
      );
    eq("invalid retry ignored"
      ,List.of(ev(null,"1",null,0,500),ev(null,"2",null,0,500),END)
      ,events
        ("retry: 500\ndata: 1\n\nretry: abc\nretry: 12x\nretry:\n"
         +"retry: -5\ndata: 2\n\n"
        )
      );
    eq("retry clamped"
      ,List.of(ev(null,"1",null,0,Integer.MAX_VALUE),END)
      ,events("retry: 99999999999\ndata: 1\n\n")
      );
  }

  static void commentsAndKeepLines()
    throws Exception
  {
    eq("comments"
      ,List.of(ka("hi"),ev(null,"1\n2",null,0,-1),END)
      ,events(": hi\ndata: 1\n: mid\ndata: 2\n\n").stream()
        .filter(e -> !(e.kind()==Kind.KEEP_ALIVE && e.text().equals("mid")))
        .toList()
      );
    List<SseEvent> got=all(whole("data: 1\n: mid\ndata: 2\n\n"),false);
    eq("comment mid-event comes first",ka("mid"),got.get(0));
    eq("event survives the comment",ev(null,"1\n2",null,0,-1),got.get(1));

    List<SseEvent> k=all(whole
      ("event: e\ndata: 1\nfoo: bar\ndata: 2\n\n: c\n"),true);
    eq("lines kept, no blank line"
      ,List.of(f("event","e"),f("data","1"),f("foo","bar"),f("data","2"))
      ,k.get(0).lines()
      );
    eq("comment line kept"
      ,List.of(new SseLine(SseLine.Kind.COMMENT,null,"c"))
      ,k.get(1).lines()
      );
    eq("lines not kept",List.of(),got.get(1).lines());
  }

  static void endOfStream()
    throws Exception
  {
    List<SseEvent> a=all(whole("data: a\n\nevent: x\ndata: b"),true);
    eq("partial event is not delivered",2,a.size());
    eq("first event",ev(null,"a",null,0,-1).data(),a.get(0).data());
    eq("end carries pending lines and the partial line"
      ,List.of(f("event","x"),new SseLine(SseLine.Kind.PARTIAL,null,"data: b"))
      ,a.get(1).lines()
      );

    List<SseEvent> b=all(whole("data: a\n\ndata: b\n"),true);
    eq("terminated field line, no blank"
      ,List.of(f("data","b"))
      ,b.get(1).lines()
      );

    eq("not kept"
      ,List.of(ev(null,"a",null,0,-1),END)
      ,all(whole("data: a\n\ndata: b"),false)
      );
    eq("clean end carries nothing"
      ,List.of(),all(whole("data: a\n\n"),true).get(1).lines()
      );

    try (SseReader r=new SseReader(whole("data: p"),true))
    {
      SseEvent e1=r.next();
      eq("end repeats",e1,r.next());
      eq("end again",e1,r.next());
    }
  }

  static void lineCap()
    throws Exception
  {
    try (SseLineReader ok=new SseLineReader(whole("0123456789\n"),10))
    { eq("line at the cap",f("0123456789",""),ok.next());
    }

    try (SseLineReader over=new SseLineReader(whole("01234567890\n"),10))
    {
      try
      { over.next(); eq("over the cap throws",true,false);
      }
      catch (IOException x)
      { eq("over the cap throws",true,true);
      }
      try
      { over.next(); eq("fails again",true,false);
      }
      catch (IOException x)
      { eq("fails again, with the cause",true,x.getCause()!=null);
      }
    }

    try (SseLineReader open=new SseLineReader(whole("0123456789ab"),10))
    {
      try
      { open.next(); eq("unterminated over the cap throws",true,false);
      }
      catch (IOException x)
      { eq("unterminated over the cap throws",true,true);
      }
    }

    String s300="x".repeat(300);
    try (SseLineReader grown=new SseLineReader(whole(s300+"\n"),300))
    { eq("grown to the cap",f(s300,""),grown.next());
    }
    
    try (SseLineReader r=new SseLineReader(whole(s300+"y\n"),300) )
    { r.next();
      eq("one over the grown cap",true,false);
    }
    catch (IOException x)
    { eq("one over the grown cap",true,true);
    }

    String big="y".repeat(100000);
    try (SseLineReader r=new SseLineReader(whole("data: "+big+"\n")))
    {
      eq("default cap allows a long line"
        ,f("data",big)
        ,r.next()
        );
    }
  }

  static void noReadAfterDispatch()
    throws Exception
  {
    SseReader r=new SseReader(strictWhole("data: x\n\n"),false);
    eq("first event without reading on"
      ,ev(null,"x",null,0,-1),r.next());
    r.close();
    
    SseReader c=new SseReader(strictWhole("data: x\r\r"),false);
    eq("CR CR without reading on",ev(null,"x",null,0,-1),c.next());
    c.close();
    
    SseReader t=new SseReader(strictTrickle("data: x\n\n"),false);
    eq("one byte reads",ev(null,"x",null,0,-1),t.next());
    t.close();
    
    SseReader tc=new SseReader(strictTrickle("data: x\r\r"),false);
    eq("one byte reads, CR CR",ev(null,"x",null,0,-1),tc.next());
    tc.close();
    
    SseReader two=new SseReader(strictWhole("data: 1\n\ndata: 2\n\n"),false);
    eq("two events in one read 1",ev(null,"1",null,0,-1),two.next());
    eq("two events in one read 2",ev(null,"2",null,0,-1),two.next());
    two.close();
  }

  static void failuresAndClose()
    throws Exception
  {
    InputStream bad=new InputStream()
    {
      int n;
      public int read()
      { throw new UnsupportedOperationException();
      }
      public int read(byte[] b,int off,int len)
        throws IOException
      {
        if (n++==0)
        { 
          byte[] x=utf8("data: 1\n\ndata: ");
          System.arraycopy(x,0,b,off,x.length);
          return x.length;
        }
        throw new IOException("boom");
      }
    };
    try (SseReader r=new SseReader(bad,false))
    {
      eq("event before the failure",ev(null,"1",null,0,-1),r.next());
      try
      { r.next(); eq("failure propagates",true,false);
      }
      catch (IOException x)
      { eq("failure propagates","boom",x.getMessage());
      }
      try
      { r.next(); eq("failure sticks",true,false);
      }
      catch (IOException x)
      { eq("failure sticks","boom",x.getCause().getMessage());
      }
    }

    Stream s=whole("data: x\n\n");
    SseReader c=new SseReader(s,false);
    c.close();
    eq("close closes the stream",true,s.closed);
  }
}