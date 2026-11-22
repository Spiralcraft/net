package spiralcraft.net.http.client;

import java.io.IOException;
import java.net.URI;

import spiralcraft.common.ContextualException;
import spiralcraft.lang.Binding;
import spiralcraft.lang.Focus;
import spiralcraft.net.mime.MimeHeader;
import spiralcraft.net.mime.MimeHeaderMap;
import spiralcraft.task.Chain;
import spiralcraft.vfs.Resolver;
import spiralcraft.vfs.Resource;
import spiralcraft.vfs.UnresolvableURIException;

/**
 * Base for plans that make an HTTP request with a new Client for each task.
 *   Defines the request as expressions bound in the context of the plan
 *   (the log level is the plan's own).
 */
public abstract class ClientPlan<Tcontext,Tresult>
  extends Chain<Tcontext,Tresult>
{
  protected Binding<URI> uriX;
  protected Binding<String> methodX;
  protected Binding<String> contentTypeX;
  protected Binding<Resource> contentX;
  protected Binding<MimeHeader[]> headersX;

  protected URI uri;
  protected String method;
  protected String contentType;
  protected URI contentURI;
  protected MimeHeader[] headers;
  protected URI headersURI;
  
  
  protected boolean debugStream;
  protected int connectTimeoutMs;
  protected int readTimeoutMs;

  /** A fixed default URI */
  public void setUri(URI uri)
  { this.uri=uri;
  }
  
  /** The URI to request. Required. */
  public void setUriX(Binding<URI> uriX)
  { this.uriX=uriX;
  }

  public void setMethod(String method)
  { this.method=method;
  }
  
  /** The method. GET if not specified or null. */
  public void setMethodX(Binding<String> methodX)
  { this.methodX=methodX;
  }

  public void setContentType(String contentType)
  { this.contentType=contentType;
  }
  
  public void setContentTypeX(Binding<String> contentTypeX)
  { this.contentTypeX=contentTypeX;
  }

  public void setContentURI(URI contentURI)
  { this.contentURI=contentURI;
  }
  
  public void setContentX(Binding<Resource> contentX)
  { this.contentX=contentX;
  }
  
  public void setHeaders(MimeHeader[] headers)
  { this.headers=headers;
  }

  public void setHeadersURI(URI uri)
  { this.headersURI=uri;
  }

  public void setHeadersX(Binding<MimeHeader[]> headersX)
  { this.headersX=headersX;
  }

  public void setDebugStream(boolean debugStream)
  { this.debugStream=debugStream;
  }

  public void setConnectTimeoutMs(int connectTimeoutMs)
  { this.connectTimeoutMs=connectTimeoutMs;
  }

  public void setReadTimeoutMs(int readTimeoutMs)
  { this.readTimeoutMs=readTimeoutMs;
  }

  /**
   * Bind the request expressions against the contextual Focus the plan is
   *   given.
   */
  @Override
  protected Focus<?> bindImports(Focus<?> importChain)
    throws ContextualException
  {
    importChain=super.bindImports(importChain);
    if (uriX==null && uri==null)
    { throw new ContextualException("uri or uriX is required",getDeclarationInfo());
    }
    if (uriX!=null)
    { uriX.bind(importChain);
    }
    if (methodX!=null)
    { methodX.bind(importChain);
    }
    if (contentTypeX!=null)
    { contentTypeX.bind(importChain);
    }
    if (contentX!=null)
    { contentX.bind(importChain);
    }
    if (headersX!=null)
    { headersX.bind(importChain);
    }
    return importChain;
  }

  /** A new Client configured from this plan. One for each task instance. */
  protected Client newClient()
  {
    Client client=new Client();
    client.setLogLevel(logLevel);
    client.setDebugStream(debugStream);
    client.setConnectTimeoutMs(connectTimeoutMs);
    client.setReadTimeoutMs(readTimeoutMs);
    return client;
  }

  protected URI uri()
  { 
    return uriX!=null?uriX.get():uri;
  }

  protected String method()
  { 
    String m=methodX!=null?methodX.get():method;
    if (logLevel.isFine())
    { log.fine("Configured request method: "+m);
    }
    return m!=null?m:"GET";
  }

  protected String contentType()
  { return contentTypeX!=null?contentTypeX.get():contentType;
  }

  protected Resource content()
  { 
    if (contentX!=null)
    { return contentX.get();
    }
    else if (contentURI!=null)
    { 
      try
      { return Resolver.getInstance().resolve(contentURI);
      }
      catch (UnresolvableURIException x)
      { throw new IllegalArgumentException("Content URI could not be resolved: "+contentURI);
      }
    }
    return null;
    
  }

  /** The headers as a map, which is empty if there are none. */
  protected MimeHeaderMap headers()
  {
    MimeHeaderMap map=new MimeHeaderMap();
    if (headersURI!=null)
    {
      try
      {
        MimeHeaderMap.read(Resolver.getInstance().resolve(headersURI), map);
        
      }
      catch (IOException x)
      { throw new IllegalArgumentException("Could not read headers resource "+headersURI,x);
      }
    }

    if (headers!=null)
    { 
      for (MimeHeader header : headers)
      { map.add(header);
      }
    }
    MimeHeader[] array=headersX!=null?headersX.get():null;
    if (array!=null)
    { 
      for (MimeHeader header : array)
      { map.add(header);
      }
    }
    if (logLevel.isFine())
    { log.fine("Configured header map: "+map.toString());
    }
    return map;
  }
}