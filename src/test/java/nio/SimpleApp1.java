package nio;

import nio.api.HttpRequest;
import nio.api.HttpResponse;
import nio.api.NioWebApp;

import java.io.IOException;

public class SimpleApp1 implements NioWebApp
{
    private static final String APP_NAME = "SimpleApp 1.0";

    public void service (HttpRequest httpRequest, HttpResponse httpResponse) throws IOException
    {
        httpResponse.addHeader ("Content-Type", "text/html");
        httpResponse.append ("<html><head><title>")
            .append (APP_NAME)
            .append ("</title></head><body>")
            .append (APP_NAME)
            .append (" got ")
            .append (httpRequest.getHttpMethod ())
            .append (" request for location ")
            .append (httpRequest.getReqLocation ())
            .append ("</body></html>");

        httpResponse.commitWriter (true);
    }

    public void start () throws IOException
    {
        System.out.println ("Starting " + APP_NAME);
    }

    public void stop () throws IOException
    {
        System.out.println ("Stopping " + APP_NAME);
    }
}
