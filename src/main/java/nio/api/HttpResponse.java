package nio.api;

import nio.NioWebServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse
{
    private static final CharsetEncoder charsetEncoder = StandardCharsets.ISO_8859_1.newEncoder ();
    private static final int RESPONSE_BUFFER = 16 * 1024;
    private final String httpVersion = "HTTP/1.1";
    private int responseCode = 200;
    private String responseReason = "OK";
    private final Map <String, String> respHeaders = new LinkedHashMap <> ();
    private final StringBuffer respBody = new StringBuffer ();
    private byte[] respBytes;
    private final SocketChannel clientChannel;

    public HttpResponse (SocketChannel clientChannel)
    {
        this.clientChannel = clientChannel;
    }

    private void addDefaultHeaders (String contentType, String serverName)
    {
        respHeaders.putIfAbsent ("Date", new Date ().toString ());
        respHeaders.putIfAbsent ("Connection", "close");

        if (contentType != null)
            respHeaders.putIfAbsent ("Content-Type", contentType);
        if (respBody.length () > 0)
            respHeaders.putIfAbsent ("Content-Length", Integer.toString (respBody.length ()));
        if (serverName != null)
            respHeaders.putIfAbsent ("Server", serverName);
    }

    public int getResponseCode ()
    {
        return responseCode;
    }

    public String getResponseReason ()
    {
        return responseReason;
    }

    public String getHttpVersion ()
    {
        return httpVersion;
    }

    public String getHeader (String header)
    {
        return respHeaders.get (header);
    }

    public StringBuffer getRespBody ()
    {
        return respBody;
    }

    public void setResponseCode (int responseCode)
    {
        this.responseCode = responseCode;
    }

    public void setResponseReason (String responseReason)
    {
        this.responseReason = responseReason;
    }

    public void addHeader (String key, String value)
    {
        respHeaders.put (key, value);
    }

    public void setBody (byte [] bodyBytes)
    {
        this.respBytes = bodyBytes;
    }

    public ByteBuffer getResponseWriter (boolean addDefaultHeaders) throws CharacterCodingException
    {
        StringBuilder fullResponse = getResponseHeaders (addDefaultHeaders);
        if (respBytes != null)
            throw new RuntimeException ("Cannot use response stream and call getResponseWriter()");
        if (respBody.length () > 0)
            fullResponse.append (respBody);

        ByteBuffer responseBuffer = ByteBuffer.allocateDirect (RESPONSE_BUFFER);
        charsetEncoder.reset ();
        responseBuffer = charsetEncoder.encode (CharBuffer.wrap (fullResponse));
        charsetEncoder.flush (responseBuffer);
        responseBuffer.flip ();
        responseBuffer.clear ();
        return responseBuffer;
    }

    public ByteBuffer getResponseStream (boolean addDefaultHeaders) throws CharacterCodingException
    {
        StringBuilder fullResponse = getResponseHeaders (addDefaultHeaders);
        if (respBody.length () > 0)
            throw new RuntimeException ("Cannot use response writer and call getResponseStream()");
        byte[] respHeaders = fullResponse.toString ().getBytes(StandardCharsets.UTF_8);

        ByteBuffer responseBuffer = ByteBuffer.allocate(respHeaders.length + respBytes.length);
        responseBuffer.put (respHeaders);
        responseBuffer.put (respBytes);

        responseBuffer.flip ();
        responseBuffer.clear ();
        return responseBuffer;
    }

    public StringBuilder getResponseHeaders (boolean addDefaultHeaders)
    {
        if (addDefaultHeaders)
            addDefaultHeaders (null, null);
        StringBuilder fullResponse = new StringBuilder ();
        fullResponse.append (httpVersion).append (" ").append (responseCode).append (" ").append (responseReason).append ("\r\n");
        for (String currHeader : respHeaders.keySet ())
        {
            fullResponse.append (currHeader).append (": ").append (respHeaders.get (currHeader)).append ("\r\n");
        }
        fullResponse.append ("\r\n");
        return fullResponse;
    }

    public StringBuffer append(String newLine)
    {
        respBody.append (newLine);
        return respBody;
    }

    public void commitWriter (boolean addDefaultHeaders) throws IOException
    {
        clientChannel.write (getResponseWriter (addDefaultHeaders));
    }

    public void commitStream (boolean addDefaultHeaders) throws IOException
    {
        clientChannel.write (getResponseStream (addDefaultHeaders));
    }
}
