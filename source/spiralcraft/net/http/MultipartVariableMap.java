package spiralcraft.net.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;

import spiralcraft.vfs.StreamUtil;

import java.util.LinkedList;

import spiralcraft.net.mime.MultipartParser;

public class MultipartVariableMap
  extends VariableMap
{
  
  private LinkedList<URI> tempFiles=new LinkedList<URI>();

  public void read(InputStream in,String contentType,int contentLength)
    throws IOException
  {
    MultipartParser parser=new MultipartParser(in,contentType,contentLength);
    int partNum=0;
    while (parser.nextPart())
    {
      String name=parser.getPartName();      
      InputStream contentIn=parser.getInputStream();

      if (parser.getPartFilename()!=null)
      { 
        add(name+".filename",parser.getPartFilename());
        add(contentType+".contentType",parser.getPartContentType());
        
        File tempFile=File.createTempFile("upload",null);
        tempFiles.add(tempFile.toURI());
        OutputStream out=new FileOutputStream(tempFile);
        StreamUtil.copyRaw(contentIn, out, 8192);
        add(name+".temporaryURI",tempFile.toURI().toString());
        
      }
      else
      { add(name,StreamUtil.readAsciiString(contentIn,-1));
      }

      partNum++;
      
    }
  }
  
  public void clear()
  { 
    super.clear();
    for (URI uri:tempFiles)
    { new File(uri).delete();
    }
    tempFiles.clear();
  }
  
  
}
