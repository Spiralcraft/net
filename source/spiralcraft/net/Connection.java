package spiralcraft.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A bidirectional network Connection.
 */
public interface Connection
{

  
  void addConnectionListener(ConnectionListener listener);
  
  void removeConnectionListener(ConnectionListener listener);

  void close()
    throws IOException;

  InputStream getInputStream()
    throws IOException;

  OutputStream getOutputStream()
    throws IOException;
}
