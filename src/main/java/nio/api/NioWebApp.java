package nio.api;

import java.io.IOException;

public interface NioWebApp
{
    void service (HttpRequest httpRequest, HttpResponse httpResponse) throws IOException;
    void start () throws IOException;
    void stop () throws IOException;
}
