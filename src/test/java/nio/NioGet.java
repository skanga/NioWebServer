package nio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

public class NioGet
{
    public static void main (String[] args)
    {
        String remoteUrl = args[0];
        fetchUrl (args, remoteUrl);
    }

    private static void fetchUrl (String[] args, String remoteUrl)
    {
        SocketChannel server = null; // Channel for reading from server
        FileOutputStream outputStream = null; // Stream to destination file
        WritableByteChannel destination; // Channel to write to it

        try
        {
            // Parse the URL. Note we use the new java.net.URI, not URL here.
            URI uri = new URI (remoteUrl);
            server = sendRequest (uri);

            // Set up an output channel to send the output to.
            if (args.length > 1)
            { // Use a specified filename
                outputStream = new FileOutputStream (args[1]);
                destination = outputStream.getChannel ();
            }
            else
                // Or wrap a channel around standard out
                destination = Channels.newChannel (System.out);

            readResponse (server, destination);
        }
        catch (Exception e)
        { // Report any errors that arise
            System.err.println (e);
            System.err.println ("Usage: java NioGet <URL> [<filename>]");
        }
        finally
        { // Close the channels and output file stream, if needed
            try
            {
                if (server != null && server.isOpen ())
                    server.close ();
                if (outputStream != null)
                    outputStream.close ();
            }
            catch (IOException e)
            {
            }
        }
    }

    private static void readResponse (SocketChannel server, WritableByteChannel destination) throws IOException
    {
        // Allocate a 32 Kilobyte byte buffer for reading the response.
        // Hopefully we'll get a low-level "direct" buffer
        ByteBuffer data = ByteBuffer.allocateDirect (32 * 1024);

        // Have we discarded the HTTP response headers yet?
        boolean skippedHeaders = false;
        // The code sent by the server
        int responseCode = -1;

        // Now loop, reading data from the server channel and writing it
        // to the destination channel until the server indicates that it
        // has no more data.
        while (server.read (data) != -1)
        { // Read data, and check for end
            data.flip (); // Prepare to extract data from buffer
/*
            // All HTTP reponses begin with a set of HTTP headers, which
            // we need to discard. The headers end with the string
            // "\r\n\r\n", or the bytes 13,10,13,10. If we haven't already
            // skipped them then do so now.
            if (!skippedHeaders)
            {
                // First, though, read the HTTP response code.
                // Assume that we get the complete first line of the
                // response when the first read() call returns. Assume also
                // that the first 9 bytes are the ASCII characters
                // "HTTP/1.1 ", and that the response code is the ASCII
                // characters in the following three bytes.
                if (responseCode == -1)
                {
                    responseCode = 100 * (data.get (9) - '0') + 10 * (data.get (10) - '0') + 1 * (data.get (11) - '0');

                    // If there was an error, report it and quit
                    // Note that we do not handle redirect responses.
                    if (responseCode < 200 || responseCode >= 300)
                    {
                        System.err.println ("HTTP Error: " + responseCode);
                        System.exit (1);
                    }
                }

                // Now skip the rest of the headers.
                try
                {
                    for (; ; )
                    {
                        if ((data.get () == 13) && (data.get () == 10) && (data.get () == 13) && (data.get () == 10))
                        {
                            skippedHeaders = true;
                            break;
                        }
                    }
                }
                catch (BufferUnderflowException e)
                {
                    // If we arrive here, it means we reached the end of
                    // the buffer and didn't find the end of the headers.
                    // There is a chance that the last 1, 2, or 3 bytes in
                    // the buffer were the beginning of the \r\n\r\n
                    // sequence, so back up a bit.
                    data.position (data.position () - 3);
                    // Now discard the headers we have read
                    data.compact ();
                    // And go read more data from the server.
                    continue;
                }
            }

 */
            // Write the data out; drain the buffer fully.
            while (data.hasRemaining ())
                destination.write (data);

            // Now that the buffer is drained, put it into fill mode
            // in preparation for reading more data into it.
            data.clear (); // data.compact() also works here
        }
    }

    private static SocketChannel sendRequest (URI uri) throws IOException
    {
        SocketAddress serverAddress = getServerSocketAddress (uri);

        // Open a SocketChannel to the server
        SocketChannel server = SocketChannel.open (serverAddress);

        String path = uri.getRawPath ();
        if (path == null || path.length () == 0)
            path = "/";

        String query = uri.getRawQuery ();
        query = (query == null) ? "" : '?' + query;

        // Put together the HTTP request we'll send to the server.
        String request = "GET " + path + query + " HTTP/1.1\r\n" + // The request
            "Host: " + uri.getHost () + "\r\n" + // Required in HTTP 1.1
            "Connection: close\r\n" + // Don't keep connection open
            "User-Agent: " + NioGet.class.getName () + "\r\n" + "\r\n";
        // Blank line indicates end of request headers

        // Now wrap a CharBuffer around that request string
        CharBuffer requestChars = CharBuffer.wrap (request);

        // Use the charset to encode the request into a byte buffer
        ByteBuffer requestBytes = StandardCharsets.ISO_8859_1.encode (requestChars);

        // Finally, we can send this HTTP request to the server.
        server.write (requestBytes);
        return server;
    }

    private static SocketAddress getServerSocketAddress (URI uri)
    {
        // Now query and verify the various parts of the URI
        String scheme = uri.getScheme ();
        if (scheme == null || !scheme.equals ("http"))
            throw new IllegalArgumentException ("Must use http protocol only");

        String hostname = uri.getHost ();

        int port = uri.getPort ();
        if (port == -1)
            port = 80; // Use default port if none specified
        // Combine the hostname and port into a single address object.
        return new InetSocketAddress (hostname, port);
    }
}
