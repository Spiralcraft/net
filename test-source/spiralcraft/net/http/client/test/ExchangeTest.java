package spiralcraft.net.http.client.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import spiralcraft.lang.SimpleFocus;
import spiralcraft.net.http.ConnectionHeader;
import spiralcraft.net.http.client.Client;
import spiralcraft.net.http.client.ClientPlan;
import spiralcraft.net.http.client.Exchange;
import spiralcraft.net.http.client.Fetch;
import spiralcraft.net.http.client.HttpConnection;
import spiralcraft.net.http.client.Request;
import spiralcraft.net.http.client.Response;
import spiralcraft.net.http.client.StreamEvents;
import spiralcraft.task.AbstractTask;
import spiralcraft.task.TaskCommand;

/**
 * Checks of Exchange, connection reuse and the cancel of Fetch and
 *   StreamEvents, against a local stand-in server on the loopback address:
 *   no network beyond it, fully automatic. A cancel during the connect
 *   itself is not checked.
 */
public class ExchangeTest
{
  static int checks;
  static int failures;

  /** Time after which a cancel is sent from another thread */
  static final int CANCEL_MS=200;

  /** A cancel must end the wait well before this */
  static final int CANCEL_LIMIT_MS=3000;

  /** Read timeout of every client: keeps a broken test from hanging */
  static final int READ_TIMEOUT_MS=10000;

  /**
   * A stand-in server. For each connection it accepts it reads one request
   *   head per reply and writes the reply; a null reply means stay silent.
   *   After the last reply it stalls until the server is closed.
   */
  static class StandIn
    implements AutoCloseable
  {
    final ServerSocket server;
    final String[] replies;
    final AtomicInteger accepts=new AtomicInteger();
    final List<Socket> sockets=new ArrayList<>();

    StandIn(String... replies)
      throws IOException
    {
      this.replies=replies;
      server=new ServerSocket(0,8,InetAddress.getLoopbackAddress());
      Thread thread=new Thread(this::serve,"ExchangeTest stand-in");
      thread.setDaemon(true);
      thread.start();
    }

    URI uri(String path)
    { return URI.create("http://127.0.0.1:"+port()+path);
    }

    int port()
    { return server.getLocalPort();
    }

    private void serve()
    {
      try
      {
        while (true)
        {
          Socket socket=server.accept();
          accepts.incrementAndGet();
          synchronized (sockets)
          { sockets.add(socket);
          }
          Thread thread=new Thread(()->converse(socket));
          thread.setDaemon(true);
          thread.start();
        }
      }
      catch (IOException x)
      { // closed
      }
    }

    private void converse(Socket socket)
    {
      try
      {
        InputStream in=socket.getInputStream();
        OutputStream out=socket.getOutputStream();
        for (String reply: replies)
        {
          if (!readHead(in))
          { return;
          }
          if (reply==null)
          { break;
          }
          out.write(reply.getBytes(StandardCharsets.ISO_8859_1));
          out.flush();
        }
        while (in.read()!=-1)
        { // stall until closed
        }
      }
      catch (IOException x)
      { // closed
      }
    }

    /** Read up to the blank line that ends a request head */
    private static boolean readHead(InputStream in)
      throws IOException
    {
      int state=0;
      while (true)
      {
        int c=in.read();
        if (c==-1)
        { return false;
        }
        if (c=='\n')
        {
          if (state==1)
          { return true;
          }
          state=1;
        }
        else if (c!='\r')
        { state=0;
        }
      }
    }

    @Override
    public void close()
      throws IOException
    {
      server.close();
      synchronized (sockets)
      {
        for (Socket socket: sockets)
        { socket.close();
        }
      }
    }
  }

  /** Failure messages of the group being run */
  static final List<String> messages=new ArrayList<>();

  static void eq(String what,Object expected,Object actual)
  {
    checks++;
    if (expected==null?actual!=null:!expected.equals(actual))
    {
      failures++;
      messages.add(what+": expected ["+expected+"] actual ["+actual+"]");
    }
  }

  static void ok(String what,boolean condition)
  { eq(what,true,condition);
  }

  static Client client()
  {
    Client client=new Client();
    client.setReadTimeoutMs(READ_TIMEOUT_MS);
    return client;
  }

  static Request get(String path)
  {
    Request request=new Request();
    request.setMethod("GET");
    request.setPath(path);
    return request;
  }

  static String read(InputStream in)
    throws IOException
  { return new String(in.readAllBytes(),StandardCharsets.ISO_8859_1);
  }

  /** Close the closeable from another thread after CANCEL_MS */
  static void cancelLater(AutoCloseable closeable)
  {
    Thread thread=new Thread
      (()->
        {
          try
          {
            Thread.sleep(CANCEL_MS);
            closeable.close();
          }
          catch (Exception x)
          { // ignore
          }
        }
      );
    thread.setDaemon(true);
    thread.start();
  }

  /** The failure of run, or null; and its time in ms in elapsed[0] */
  static Exception timed(Check run,long[] elapsed)
  {
    long start=System.currentTimeMillis();
    try
    {
      run.run();
      return null;
    }
    catch (Exception x)
    { return x;
    }
    finally
    { elapsed[0]=System.currentTimeMillis()-start;
    }
  }

  interface Check
  { void run() throws Exception;
  }

  static final String[] GROUPS=
    { "complete","stream","cancelHead","cancelBody","reuse","guard"
    , "keepalive","status","taskStop"
    };

  /**
   * Runs one group of checks and returns its failure messages, empty if
   *   they all passed. Called by the test assembly (ExchangeTest.assy.xml).
   */
  public static List<String> group(String name)
    throws Exception
  {
    messages.clear();
    switch (name)
    {
      case "complete": complete(); break;
      case "stream": stream(); break;
      case "cancelHead": cancelHead(); break;
      case "cancelBody": cancelBody(); break;
      case "reuse": reuse(); break;
      case "guard": guard(); break;
      case "keepalive": keepalive(); break;
      case "status": status(); break;
      case "taskStop": taskStop(); break;
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

  /** complete() buffers; the content outlives the exchange */
  static void complete()
    throws Exception
  {
    try (StandIn server
          =new StandIn("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello")
        )
    {
      Exchange exchange=client().open("GET",server.uri("/"),null,null,null);
      eq("no response before start",null,exchange.getResponse());
      Response response=exchange.complete();
      eq("status",200,response.getStatus());
      eq("content","hello",response.getContentAsString());
      ok("closed after complete",exchange.isClosed());
      response.close();
      eq("content after close","hello",response.getContentAsString());
      try
      {
        exchange.complete();
        ok("second complete refused",false);
      }
      catch (IllegalStateException x)
      { ok("second complete refused",true);
      }
      eq("buffered response kept"
        ,"hello"
        ,exchange.getResponse().getContentAsString()
        );
      try
      {
        exchange.start();
        ok("second start refused",false);
      }
      catch (IllegalStateException x)
      { ok("second start refused",true);
      }
    }

    try (StandIn server=new StandIn("HTTP/1.1 204 No Content\r\n\r\n"))
    {
      Response response=client().executeRequest(server.uri("/"));
      eq("204 status",204,response.getStatus());
      eq("204 content","",response.getContentAsString());
    }
  }

  /** start() streams; reading to the end releases */
  static void stream()
    throws Exception
  {
    try (StandIn server
          =new StandIn
            ("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
            +"3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n"
            )
        )
    {
      Exchange exchange=client().open("GET",server.uri("/"),null,null,null);
      InputStream in=exchange.start().getInputStream();
      ok("open while streaming",!exchange.isClosed());
      eq("body","abcde",read(in));
      ok("released at the end",exchange.isClosed());
      eq("read after the end",-1,in.read());
      try
      {
        exchange.complete();
        ok("complete after stream refused",false);
      }
      catch (IllegalStateException x)
      { ok("complete after stream refused",true);
      }
    }
  }

  /** A cancel while waiting for the response head */
  static void cancelHead()
    throws Exception
  {
    try (StandIn server=new StandIn((String) null))
    {
      Exchange exchange=client().open("GET",server.uri("/"),null,null,null);
      cancelLater(exchange);
      long[] elapsed=new long[1];
      Exception x=timed(exchange::start,elapsed);
      ok("head wait fails with IOException",x instanceof IOException);
      ok("head wait cancelled",exchange.isClosed());
      ok("head wait ended by the cancel",elapsed[0]<CANCEL_LIMIT_MS);
    }
  }

  /** A cancel while the body is read, buffered and streamed */
  static void cancelBody()
    throws Exception
  {
    String stall="HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\npartial";
    try (StandIn server=new StandIn(stall))
    {
      Exchange exchange=client().open("GET",server.uri("/"),null,null,null);
      exchange.start();
      cancelLater(exchange);
      long[] elapsed=new long[1];
      Exception x=timed(exchange::complete,elapsed);
      ok("complete fails with IOException",x instanceof IOException);
      ok("complete cancelled",exchange.isClosed());
      ok("complete ended by the cancel",elapsed[0]<CANCEL_LIMIT_MS);
      ok("stream closed",exchange.getResponse().getInputStream().isClosed());
    }

    try (StandIn server=new StandIn(stall))
    {
      Exchange exchange=client().open("GET",server.uri("/"),null,null,null);
      Response response=exchange.start();
      // Response.close() is the cancel too
      cancelLater(response);
      long[] elapsed=new long[1];
      Exception x=timed(()->read(response.getInputStream()),elapsed);
      ok("stream read fails with IOException",x instanceof IOException);
      ok("stream read cancelled",exchange.isClosed());
      ok("stream read ended by the cancel",elapsed[0]<CANCEL_LIMIT_MS);
    }
  }

  /** Exchanges one after another on a connection the caller holds */
  static void reuse()
    throws Exception
  {
    try (StandIn server
          =new StandIn
            ("HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\none"
            ,"HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
              +"3\r\ntwo\r\n0\r\n\r\n"
            ,"HTTP/1.1 200 OK\r\nConnection: Keep-Alive, Close\r\n"
              +"Content-Length: 5\r\n\r\nthree"
            )
        )
    {
      Client client=client();
      HttpConnection connection=new HttpConnection("127.0.0.1",server.port());
      connection.setReadTimeoutMs(READ_TIMEOUT_MS);
      ok("reusable before the first",connection.isReusable());

      Response one=client.open(connection,get("/1")).complete();
      eq("first","one",one.getContentAsString());
      ok("reusable after the first",connection.isReusable());

      InputStream in=client.open(connection,get("/2")).start().getInputStream();
      eq("second, streamed","two",read(in));
      ok("reusable after the second",connection.isReusable());

      Response three=client.open(connection,get("/3")).complete();
      eq("third","three",three.getContentAsString());
      ok("closed on the close option",connection.isClosed());
      ok("not reusable after close",!connection.isReusable());
      eq("one connection",1,server.accepts.get());

      try
      {
        client.open(connection,get("/4")).start();
        ok("exchange on a closed connection fails",false);
      }
      catch (IOException x)
      { ok("exchange on a closed connection fails",true);
      }
    }

    try (StandIn server
          =new StandIn("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok")
        )
    {
      // The Client's own connection is used once (Connection: close)
      HttpConnection connection=new HttpConnection("127.0.0.1",server.port());
      Request request=get("/");
      request.addHeader(ConnectionHeader.CLOSE);
      client().open(connection,request).complete();
      ok("closed when the request asked",connection.isClosed());
    }
  }

  /** One exchange at a time on a connection */
  static void guard()
    throws Exception
  {
    try (StandIn server
          =new StandIn("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nfirst")
        )
    {
      Client client=client();
      HttpConnection connection=new HttpConnection("127.0.0.1",server.port());
      connection.setReadTimeoutMs(READ_TIMEOUT_MS);
      Exchange first=client.open(connection,get("/1"));
      Response response=first.start();
      Exchange second=client.open(connection,get("/2"));
      try
      {
        second.start();
        ok("second refused while the first runs",false);
      }
      catch (IllegalStateException x)
      { ok("second refused while the first runs",true);
      }
      ok("connection left open",!connection.isClosed());
      ok("first not closed",!first.isClosed());
      eq("first still reads","first",read(response.getInputStream()));
    }
  }

  static Response head(String text)
    throws IOException
  {
    Response response=new Response();
    response.start
      (new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
    return response;
  }

  /** Response.isKeepalive() by RFC 9112 9.3 */
  static void keepalive()
    throws Exception
  {
    ConnectionHeader.init();
    ok("HTTP/1.1 persists"
      ,head("HTTP/1.1 200 OK\r\n\r\n").isKeepalive()
      );
    ok("HTTP/1.1 close"
      ,!head("HTTP/1.1 200 OK\r\nConnection: close\r\n\r\n").isKeepalive()
      );
    ok("close among options, any case"
      ,!head("HTTP/1.1 200 OK\r\nConnection: Upgrade, CLOSE\r\n\r\n")
        .isKeepalive()
      );
    ok("HTTP/1.0 closes"
      ,!head("HTTP/1.0 200 OK\r\n\r\n").isKeepalive()
      );
    ok("HTTP/1.0 keep-alive"
      ,head("HTTP/1.0 200 OK\r\nConnection: Keep-Alive\r\n\r\n").isKeepalive()
      );
  }

  /** The status line: the reason may be empty or missing */
  static void status()
    throws Exception
  {
    Response response=head("HTTP/1.1 200 OK\r\n\r\n");
    eq("status",200,response.getStatus());
    eq("reason","OK",response.getReason());
    eq("protocol","HTTP/1.1",response.getProtocol());

    response=head("HTTP/1.1 404 Not Found\r\n\r\n");
    eq("a reason of two words","Not Found",response.getReason());
    eq("its status",404,response.getStatus());

    response=head("HTTP/1.1 200 \r\n\r\n");
    eq("empty reason, space kept: status",200,response.getStatus());
    eq("empty reason, space kept: reason","",response.getReason());

    response=head("HTTP/1.1 204\r\n\r\n");
    eq("no reason, no space: status",204,response.getStatus());
    eq("no reason, no space: reason","",response.getReason());

    response=head("HTTP/1.1 200 \r\nX-Test: yes\r\n\r\n");
    eq("headers after an empty reason","yes"
      ,response.getHeader("X-Test").getRawValue());

    try
    { 
      head("HTTP/1.1\r\n\r\n");
      ok("a line with no status code is refused",false);
    }
    catch (IOException x)
    { ok("a line with no status code is refused",true);
    }
  }

  /** Run plan's task; stop it from another thread first if stop */
  static Exception runTask
    (ClientPlan<Void,Void> plan,boolean stop,long[] elapsed)
    throws Exception
  {
    plan.setReadTimeoutMs(READ_TIMEOUT_MS);
    plan.bind(new SimpleFocus<Void>());
    TaskCommand<?,?> command=plan.command();
    if (stop)
    { cancelLater(()->command.getTask().stop());
    }
    long start=System.currentTimeMillis();
    command.execute();
    elapsed[0]=System.currentTimeMillis()-start;
    return ((AbstractTask) command.getTask()).getException();
  }

  /** stop() cancels Fetch and StreamEvents; a real failure is reported */
  static void taskStop()
    throws Exception
  {
    long[] elapsed=new long[1];
    try (StandIn server=new StandIn((String) null))
    {
      Fetch fetch=new Fetch();
      fetch.setUri(server.uri("/"));
      Exception x=runTask(fetch,true,elapsed);
      eq("stopped Fetch reports nothing",null,x);
      ok("Fetch ended by the stop",elapsed[0]<CANCEL_LIMIT_MS);
    }

    try (StandIn server=new StandIn((String) null))
    {
      StreamEvents events=new StreamEvents();
      events.setUri(server.uri("/"));
      Exception x=runTask(events,true,elapsed);
      eq("stopped StreamEvents reports nothing",null,x);
      ok("StreamEvents ended by the stop",elapsed[0]<CANCEL_LIMIT_MS);
    }

    try (StandIn server
          =new StandIn
            ("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n\r\n"
            +"data: one\n\n"
            )
        )
    {
      StreamEvents events=new StreamEvents();
      events.setUri(server.uri("/"));
      Exception x=runTask(events,true,elapsed);
      eq("StreamEvents stopped mid-stream reports nothing",null,x);
      ok("StreamEvents mid-stream ended by the stop"
        ,elapsed[0]<CANCEL_LIMIT_MS
        );
    }

    int port;
    try (ServerSocket closed=new ServerSocket(0))
    { port=closed.getLocalPort();
    }
    Fetch fetch=new Fetch();
    fetch.setUri(URI.create("http://127.0.0.1:"+port+"/"));
    ok("refused connection reported",runTask(fetch,false,elapsed)!=null);
  }
}
