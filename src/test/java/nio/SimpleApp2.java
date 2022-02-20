package nio;

import nio.api.HttpRequest;
import nio.api.HttpResponse;
import nio.api.NioWebApp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SimpleApp2 implements NioWebApp
{
    private static final String APP_NAME = "SimpleApp 2.0";

    public void service (HttpRequest httpRequest, HttpResponse httpResponse) throws IOException
    {
        httpResponse.addHeader ("Content-Type", "text/html");
        String respString = "<html><head><title>" + APP_NAME + "</title></head><body>"
            + APP_NAME
            + " got "
            + httpRequest.getHttpMethod ()
            + " request for location "
            + httpRequest.getReqLocation ()
            + (httpRequest.getHttpMethod ().equalsIgnoreCase ("POST") ? " with POST params: " + httpRequest.getPostParams () : "")
            + "</body></html>";
        byte[] respBytes = respString.getBytes(StandardCharsets.UTF_8);
        httpResponse.setBody (respBytes);
        httpResponse.commitStream (true);
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
