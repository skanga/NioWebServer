package nio;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

public class NioPooledWebServerTest
{
    private static String listenHost = "localhost";
    private static int listenPort = 8080;
    private static NioPooledWebServer theServer;

    @BeforeClass
    public static void setUp () throws Exception
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    theServer = new NioPooledWebServer();
                    theServer.startServer (listenHost, listenPort, "nio");
                }
                catch (Exception e)
                {
                    e.printStackTrace ();
                }
            }
        }).start();
    }

    @AfterClass
    public static void tearDown () throws Exception
    {
        theServer.stopAllApps ();
    }

    @Test
    public void testGetRoot() throws Exception
    {
        TestUtils.HttpResponse getResp = TestUtils.getUrl ("http://localhost:8080/", null);
        Assert.assertEquals ("<html><head><title>Java NIO Webserver 1.0</title></head><body>Java NIO Webserver 1.0 got GET request for location /</body></html>", getResp.getResponseString());
    }

    @Test
    public void testPostRoot() throws Exception
    {
        HashMap <String, String> postParams = new HashMap <> ();
        postParams.put ("user", "user1");
        postParams.put ("client_secret", "secret");
        TestUtils.HttpResponse getResp = TestUtils.postUrl ("http://localhost:8080/", postParams, null);
        Assert.assertEquals ("<html><head><title>Java NIO Webserver 1.0</title></head><body>Java NIO Webserver 1.0 got POST request for location /</body></html>", getResp.getResponseString());
    }

    @Test
    public void testGetSimple1() throws Exception
    {
        TestUtils.HttpResponse getResp = TestUtils.getUrl ("http://localhost:8080/SimpleApp1", null);
        Assert.assertEquals ("<html><head><title>SimpleApp 1.0</title></head><body>SimpleApp 1.0 got GET request for location /SimpleApp1</body></html>", getResp.getResponseString());
    }

    @Test
    public void testPostSimple1() throws Exception
    {
        HashMap <String, String> postParams = new HashMap <> ();
        postParams.put ("user", "user1");
        postParams.put ("client_secret", "secret");
        TestUtils.HttpResponse getResp = TestUtils.postUrl ("http://localhost:8080/SimpleApp1", postParams, null);
        Assert.assertEquals ("<html><head><title>SimpleApp 1.0</title></head><body>SimpleApp 1.0 got POST request for location /SimpleApp1</body></html>", getResp.getResponseString());
    }

    @Test
    public void testGetSimple2() throws Exception
    {
        TestUtils.HttpResponse getResp = TestUtils.getUrl ("http://localhost:8080/SimpleApp2", null);
        Assert.assertEquals ("<html><head><title>SimpleApp 2.0</title></head><body>SimpleApp 2.0 got GET request for location /SimpleApp2</body></html>", getResp.getResponseString());
    }

    @Test
    public void testPostSimple2() throws Exception
    {
        HashMap <String, String> postParams = new HashMap <> ();
        postParams.put ("user", "user1");
        postParams.put ("client_secret", "secret");
        TestUtils.HttpResponse getResp = TestUtils.postUrl ("http://localhost:8080/SimpleApp2", postParams, null);
        Assert.assertEquals ("<html><head><title>SimpleApp 2.0</title></head><body>SimpleApp 2.0 got POST request for location /SimpleApp2 with POST params: {client_secret=secret, user=user1}</body></html>", getResp.getResponseString());
    }
}