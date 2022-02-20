package nio;

import nio.api.HttpRequest;
import nio.api.HttpResponse;
import nio.util.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/*
curl -i  http://localhost:8080/
curl -i  http://localhost:8080/SimpleApp
curl -i  http://localhost:8080/SimpleApp2
curl -i -d "user=user1&pass=abcd" -X POST http://localhost:8080/
curl -i -d "user=user1&pass=abcd" -X POST http://localhost:8080/SimpleApp
curl -i -d "user=user1&pass=abcd" -X POST http://localhost:8080/SimpleApp2
*/
public class NioWebServer
{
    static String listenHost = "localhost";
    static int listenPort = 8080;
    static Selector serverSelector;
    static final String SERVER_NAME = "Java NIO Webserver 1.0";
    String appPackage = "nio";
    final ConcurrentHashMap <String, Pair <Object, Method>> appCache = new ConcurrentHashMap <> ();

    public static void main (String[] args) throws IOException
    {
        new NioWebServer ().startServer (listenHost, listenPort, null);
    }

    public void startServer (String listenHost, int listenPort, String appPackage) throws IOException
    {
        System.out.println ("Listening on port " + listenPort);
        if (appPackage != null)
            this.appPackage = appPackage;
        ServerSocketChannel serverChannel = getServerChannel (listenHost, listenPort);
        handleSelector (serverChannel);
    }

    private void handleSelector (ServerSocketChannel serverChannel) throws IOException
    {
        while (true)
        {
            // This may block for a long time. Upon returning, the
            // selected set contains keys of the ready channels.
            int numKeys = serverSelector.select ();
            if (numKeys == 0)
                continue;    // nothing to do

            // Get an iterator over the set of selected keys
            Set <SelectionKey> selectionKeys = serverSelector.selectedKeys ();
            Iterator <SelectionKey> keyIterator = selectionKeys.iterator ();

            // Look at each key in the selected set
            while (keyIterator.hasNext ())
            {
                SelectionKey selectionKey = keyIterator.next ();

                // Is a new connection coming in?
                if (selectionKey.isAcceptable ())
                {
                    // New client has been accepted
                    handleAccept (serverChannel, serverSelector);
                }
                // Is there data to read on this channel?
                else if (selectionKey.isReadable ())
                {
                    // We can run non-blocking operation READ on our client
                    handleRead (selectionKey);
                }

                // Remove key from selected set; it's been handled
                keyIterator.remove ();
            }
        }
    }

    // Set connection host, port and non-blocking mode
    private ServerSocketChannel getServerChannel (String listenHost, int listenPort) throws IOException
    {
        // Allocate an unbound server socket channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open ();
        // Get the associated ServerSocket to bind it with
        ServerSocket serverSocket = serverChannel.socket ();
        // Create a new Selector for use below
        serverSelector = Selector.open ();

        // Set the port the server channel will listen to
        serverSocket.bind (new InetSocketAddress (listenHost, listenPort));

        // Set nonblocking mode for the listening socket
        serverChannel.configureBlocking (false);

        // Register the ServerSocketChannel with the Selector
        serverChannel.register (serverSelector, SelectionKey.OP_ACCEPT);
        //serverChannel.register (serverSelector, serverChannel.validOps (), null);
        return serverChannel;
    }

    // Accept the connection and set non-blocking mode
    private void handleAccept (ServerSocketChannel serverChannel, Selector socketSelector) throws IOException
    {
        SocketChannel socketChannel = serverChannel.accept ();
        if (socketChannel != null)
        {
            // Set the new channel nonblocking
            socketChannel.configureBlocking (false);

            // Register the channel with the selector
            socketChannel.register (socketSelector, SelectionKey.OP_READ);
        }
    }

    boolean processApp (String appName, HttpRequest httpRequest, HttpResponse httpResponse)
    {
        String fullAppName = appPackage + "." + appName;  // TODO: For now one package ONLY
        try
        {
            Object appInstance;
            Method serviceMethod;
            if (appCache.get (fullAppName) == null)
            {
                Class <?> appClass = Class.forName (fullAppName);
                appInstance = appClass.getDeclaredConstructor ().newInstance ();

                Method startMethod = appClass.getMethod ("start");
                startMethod.invoke (appInstance);

                serviceMethod = appClass.getMethod ("service", HttpRequest.class, HttpResponse.class);
                appCache.put (fullAppName, Pair.of (appInstance, serviceMethod));
            }
            else
            {
                Pair<Object, Method> cachedApp = appCache.get (fullAppName);
                appInstance = cachedApp.getFirstItem ();
                serviceMethod = cachedApp.getSecondItem ();
            }
            serviceMethod.invoke(appInstance, httpRequest, httpResponse);
            return true;
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            //e.printStackTrace ();
            System.out.println (e.toString ());
            return false;
        }
    }

    public void stopAllApps () throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException
    {
        for (Map.Entry<String, Pair <Object, Method>> entry : appCache.entrySet())
        {
            String fullAppName = entry.getKey();
            Pair<Object, Method> cachedApp = entry.getValue();
            Object appInstance = cachedApp.getFirstItem ();
            Class <?> appClass = Class.forName (fullAppName);
            Method stopMethod = appClass.getMethod ("stop");
            stopMethod.invoke (appInstance);
        }
    }

    void processNotFound (HttpRequest httpRequest, HttpResponse httpResponse) throws IOException
    {
        httpResponse.append ("<html><head><title>").append (SERVER_NAME).append ("</title></head><body>").append (SERVER_NAME).append (" got ").append (httpRequest.getHttpMethod ()).append (" request for location ").append (httpRequest.getReqLocation ()).append ("</body></html>");
        httpResponse.commitWriter (true);
    }

    void readChannelFully (SelectionKey selectionKey) throws IOException
    {
        // create a ServerSocketChannel to read the request
        SocketChannel clientChannel = (SocketChannel) selectionKey.channel ();

        // Create buffer to read data
        ByteBuffer reqBuffer = ByteBuffer.allocate (1024);
        clientChannel.read (reqBuffer);
        // Parse data from buffer to String
        String reqData = new String (reqBuffer.array ()).trim ();
        if (reqData.length () > 0)
        {
            HttpRequest httpRequest = new HttpRequest (reqData);
            HttpResponse httpResponse = new HttpResponse (clientChannel);
            boolean appFound = processApp (httpRequest.getAppName (), httpRequest, httpResponse);
            if (!appFound)
                processNotFound (httpRequest, httpResponse);
            clientChannel.close ();
        }
    }

    void handleRead (SelectionKey selectionKey) throws IOException
    {
        readChannelFully (selectionKey);
    }
}
