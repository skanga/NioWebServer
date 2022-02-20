# NioWebServer

This is a simple Web / Application Server that is built upon Java NIO

This server has 2 implementations

1. NioWebServer is a standard server which uses select to asynchronously handle all incoming connections
2. NioPooledWebServer is an extension of the NIO Webserver which uses a thread pool to service the channels

All applications must implement a simple interface called NioWebApp (which is analogous to Servlet in the JEE world)
```
public interface NioWebApp
{
    void service (HttpRequest httpRequest, HttpResponse httpResponse) throws IOException;
    void start () throws IOException;
    void stop () throws IOException;
}
```

Here is an example of a simple demo application (taken from the Unit tests)
```
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
```
