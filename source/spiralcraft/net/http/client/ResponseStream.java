package spiralcraft.net.http.client;

import java.io.IOException;
import java.io.InputStream;

/**
 * Streams an HTTP response body
 */
public class ResponseStream
  extends InputStream
{
  enum Mode { NONE, LENGTH, CHUNKED, EOF }

  /** Longest chunk-size or trailer line accepted, in bytes */
  private static final int MAX_LINE_LENGTH=8192;

  /** Most trailer lines accepted after the last chunk */
  private static final int MAX_TRAILER_LINES=100;

  /** Most hex digits in a chunk size: 15 keeps it a positive long */
  private static final int MAX_SIZE_DIGITS=15;
  
  private final InputStream in;
  private final HttpConnection connection;
  private final Mode mode;
  private final Runnable onEnd;
  private long remaining;
  private boolean firstChunk=true;
  private boolean done;
  private volatile boolean closed;
  private boolean read;
  private byte[] buff1=new byte[1];

  ResponseStream
    (InputStream in,HttpConnection connection,Response response
    ,boolean headRequest,Runnable onEnd
    )
  { 
    this.in=in;
    this.connection=connection;
    this.onEnd=onEnd;
    long contentLength=response.getContentLength();
    
    int status=response.getStatus();
    if (headRequest 
        || status<200 
        || status==204 
        || status==304
        )        
    { 
      this.mode=Mode.NONE;
      done=true;
    }
    else if (response.isChunkedTransferCoding())
    { this.mode=Mode.CHUNKED;
    }
    else if (response.getHeader("Transfer-Encoding")!=null)
    { this.mode=Mode.EOF;   // TE without final chunked
    }    
    else if (contentLength ==0)
    {
      this.mode=Mode.NONE;
      done=true;
    }
    else if (contentLength > 0)
    { 
      this.mode=Mode.LENGTH;
      remaining = contentLength;
    }
    else 
    { this.mode=Mode.EOF;
    }
  }

  @Override
  public int read()
    throws IOException
  {
    
    int n=read(buff1,0,1);
    return n==-1?-1:(buff1[0] & 0xFF);
  }

  @Override
  public int read(byte[] buf,int off,int len)
    throws IOException
  {
    if (closed)
    { throw new IOException("Stream closed");
    }
    read=true;
    if (done || len==0)
    { return done?-1:0;
    }
    if (mode==Mode.CHUNKED && remaining==0 && !nextChunk())
    { return -1;
    }
    int max
      =mode==Mode.EOF?len:(int) Math.min(len,remaining);
    int n=in.read(buf,off,max);
    if (n==-1)
    {
      if (mode==Mode.EOF)
      { finish(); return -1;
      }
      throw new IOException("Premature end of response body");
    }
    if (mode!=Mode.EOF)
    {
      remaining-=n;
      if (mode==Mode.LENGTH && remaining==0)
      { finish();
      }
    }
    return n;
  }

  /** Advance to the next chunk; false at the zero-size chunk */
  private boolean nextChunk()
    throws IOException
  {
    if (!firstChunk)
    { expectCRLF();          // the CRLF ending the previous chunk
    }
    firstChunk=false;
    long size=readChunkSize(); // hex; ignore ";ext"; trim
    if (size==0)
    { readTrailers(); finish(); return false;
    }
    remaining=size;
    return true;
  }

  /** The body was read to its end: the connection is left to onEnd */
  private void finish()
  {
    done=true;
    if (onEnd!=null)
    { onEnd.run();
    }
  }

  /**
   * Close the stream. Before the end of the body this closes the
   *   connection, without draining: it is also the cancel.
   */
  @Override
  public void close()
  {
    if (!closed)
    {
      closed=true;
      if (!done)
      { connection.close();
      }
    }
  }

  /** Whether the body was read to its end, or has none */
  boolean isDone()
  { return done;
  }

  /** Whether the body ends only where the connection closes */
  boolean isEofDelimited()
  { return mode==Mode.EOF;
  }

  public boolean isClosed()
  { return closed;
  }

  /** Whether read() was called */
  boolean wasRead()
  { return read;
  }
  
  /**
   * Read the chunk-size line and return the size. A chunk extension
   *   (";name=value") and surrounding whitespace are ignored.
   */
  private long readChunkSize()
    throws IOException
  {
    String line=readLine();
    int semi=line.indexOf(';');
    if (semi>=0)
    { line=line.substring(0,semi);
    }
    line=line.trim();
    if (line.isEmpty())
    { throw new IOException("Missing chunk size");
    }
    int start=0;
    for (int i=0;i<line.length();i++)
    {
      if (Character.digit(line.charAt(i),16)<0)
      { throw new IOException("Bad chunk size ["+line+"]");
      }
      if (start==i && line.charAt(i)=='0' && i<line.length()-1)
      { start++;   // leading zero
      }
    }
    if (line.length()-start>MAX_SIZE_DIGITS)
    { throw new IOException("Chunk size too large ["+line+"]");
    }
    return Long.parseLong(line.substring(start),16);
  }  
  
  /**
   * Read the line break that ends a chunk's data: CRLF, or a bare LF,
   *   which RFC 9112 2.2 lets a recipient accept.
   */
  private void expectCRLF()
    throws IOException
  {
    int c=in.read();
    if (c=='\r')
    { c=in.read();
    }
    if (c!='\n')
    { 
      throw new IOException
        (c==-1
        ?"Premature end of chunked body"
        :"Expected CRLF after chunk data"
        );
    }
  }  
  
  /**
   * Read the trailer section after the zero-size chunk, up to and
   *   including the empty line that ends it. Trailer fields are
   *   discarded for now; parse them with MimeHeader.parse to keep them.
   */
  private void readTrailers()
    throws IOException
  {
    for (int count=0;;count++)
    {
      if (count>MAX_TRAILER_LINES)
      { throw new IOException("Too many trailer lines");
      }
      if (readLine().isEmpty())
      { return;
      }
    }
  }  
  
  
  /**
   * Read one line as US-ASCII, without its line break. CRLF ends a
   *   line; a bare LF is accepted too (RFC 9112 2.2).
   */
  private String readLine()
    throws IOException
  {
    StringBuilder line=new StringBuilder();
    while (true)
    {
      int c=in.read();
      if (c==-1)
      { throw new IOException("Premature end of chunked body");
      }
      if (c=='\n')
      { return line.toString();
      }
      if (c=='\r')
      {
        if (in.read()!='\n')
        { throw new IOException("Expected LF after CR");
        }
        return line.toString();
      }
      if (line.length()>=MAX_LINE_LENGTH)
      { throw new IOException("Line too long in chunked body");
      }
      line.append((char) c);
    }
  }  
}