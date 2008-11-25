package spiralcraft.net.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;

import spiralcraft.vfs.StreamUtil;

import java.util.LinkedList;

import spiralcraft.log.ClassLog;
import spiralcraft.io.NullOutputStream;
import spiralcraft.net.mime.MultipartParser;

public class MultipartVariableMap
  extends VariableMap
{
  private static final boolean DEBUG=false;
  
  private static final ClassLog log
    =ClassLog.getInstance(MultipartVariableMap.class);
  
  
  private LinkedList<URI> tempFiles=new LinkedList<URI>();

  public void read(InputStream in,String contentType,int contentLength)
    throws IOException
  {
    MultipartParser parser=new MultipartParser(in,contentType,contentLength);
    parser.setQuotableChars("\\\"");
    int partNum=0;
    while (parser.nextPart())
    {
      String name=parser.getPartName();      
      InputStream contentIn=parser.getInputStream();
      
      String partFilename=parser.getPartFilename();
      if (partFilename!=null)
      { 
        if (partFilename.length()>0)
        {
          // Fix IE mishegas- sends full windows specific path
          int slashPos=partFilename.lastIndexOf('\\');
          if (slashPos>-1)
          { partFilename=partFilename.substring(slashPos+1);
          }
          if (DEBUG)
          { log.fine("Filename: "+partFilename);
          }
        
          add(name+".filename",partFilename);
          add(name+".contentType",parser.getPartContentType());
        
          File tempFile=File.createTempFile("upload",null);
          tempFiles.add(tempFile.toURI());
          OutputStream out=new FileOutputStream(tempFile);
          StreamUtil.copyRaw(contentIn, out, 8192);
          add(name+".temporaryURI",tempFile.toURI().toString());
          if (DEBUG)
          { log.fine("File: "+name+"="+partFilename+":"+tempFile.toURI());
          }
        }
        else
        { StreamUtil.copyRaw(contentIn,new NullOutputStream(),8192);
        }
      }
      else
      { 
        String content=StreamUtil.readAsciiString(contentIn,-1);
//        log.fine(name+"="+content);
        add(name,content);
      }

      partNum++;
      
    }
  }
  
  @Override
  public void clear()
  { 
    super.clear();
    for (URI uri:tempFiles)
    { new File(uri).delete();
    }
    tempFiles.clear();
  }
  
  
}
