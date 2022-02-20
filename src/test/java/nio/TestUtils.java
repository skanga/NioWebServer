package nio;

import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class TestUtils
{
    /**
     * Assert that a string starts and with the specified string
     */
    public static void assertStartsWith(String actual, String expectedStart)
    {
        Assert.assertTrue(trimLines(actual).startsWith(trimLines(expectedStart)));
    }

    /**
     * Assert that a string starts and ends with the specified strings
     */
    public static void assertStartEnd(String actual, String expectedStart, String expectedEnd)
    {
        Assert.assertTrue(trimLines(actual).startsWith(trimLines(expectedStart)));
        Assert.assertTrue(trimLines(actual).endsWith(trimLines(expectedEnd)));
    }

    /**
     * Assert that a string contains a specified string
     */
    public static void assertContains(String fullString, String lookFor)
    {
        Assert.assertTrue(fullString.contains(lookFor));
    }

    /**
     * Assert that a byte[] contains a specified string
     */
    public static void assertContains(byte[] inBytes, String lookFor)
    {
        assertContains (new String(inBytes, StandardCharsets.UTF_8), lookFor);
    }

    /**
     * Assert that a string does not contain a specified string
     */
    public static void assertNotContains(String fullString, String lookFor)
    {
        Assert.assertFalse(fullString.contains(lookFor));
    }

    /**
     * Remove all CR and LR characters from a string
     */
    public static String trimLines(String inString)
    {
        inString = inString.replaceAll("\r", "");
        inString = inString.replaceAll("\n", "");
        return inString;
    }

    /**
     * Copy Reader to Writer out until EOF or exception.
     *
     * @param inReader  the Reader to read from
     * @param outWriter the Writer to write to
     * @throws IOException if unable to copy the contents
     */
    public static void copy(Reader inReader, Writer outWriter) throws IOException
    {
        final int BUFFER_SIZE = 64 * 1024;

        char[] copyBuffer = new char[BUFFER_SIZE];
        int readLen;

        while(true)
        {
            readLen = inReader.read(copyBuffer, 0, BUFFER_SIZE);
            if(readLen == -1)
                break;

            outWriter.write(copyBuffer, 0, readLen);
        }
    }

    /**
     * Copy the entire InputStream to the OutputStream
     *
     * @param inStream  the input stream to read from
     * @param outStream the output stream to write to
     * @throws IOException if unable to copy the stream
     */
    public static void copy(InputStream inStream, OutputStream outStream) throws IOException
    {
        final int BUFFER_SIZE = 64 * 1024;
        byte[] copyBuffer = new byte[BUFFER_SIZE];
        int readLen;

        while(true)
        {
            readLen = inStream.read(copyBuffer, 0, BUFFER_SIZE);
            if(readLen < 0)
                break;

            outStream.write(copyBuffer, 0, readLen);
        }
    }

    /**
     * Compute the SHA-1 checksum for a byte array
     */
    public static String sha1Sum(byte[] inputBytes) throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        Formatter byteFormatter = new Formatter();
        for(byte currByte : messageDigest.digest(inputBytes))
        {
            byteFormatter.format("%02x", currByte);
        }
        return byteFormatter.toString();
    }

    public static File[] listFilesForFolder(final File folderLoc, final String nameFilter, final String extFilter)
    {
        File[] fileMatches = folderLoc.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                if (nameFilter == null)
                    return name.endsWith("." + extFilter);
                if (extFilter == null)
                    return name.startsWith(nameFilter);

                return name.startsWith(nameFilter) && name.endsWith("." + extFilter);
            }
        });
        return fileMatches;
    }

    /**
     * Read the contents of a file into a String and return it.
     *
     * @param inFile the file to read.
     * @return the contents of the file.
     * @throws IOException if unable to read the file.
     */
    public static String readToString(File inFile) throws IOException
    {
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader(inFile);
            StringWriter stringWriter = new StringWriter();
            copy(fileReader, stringWriter);
            return stringWriter.toString();
        }
        finally
        {
            close(fileReader);
        }
    }

    /**
     * Read the contents of a stream into a byte array and return it.
     */
    public static byte[] readToBytes(InputStream inStream) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        for(int count; (count = inStream.read(buffer)) > 0; )
        {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }


    /**
     * Read the contents of a file into a String and return it.
     *
     * @param inFile the file to read.
     * @return the contents of the file.
     * @throws IOException if unable to read the file.
     */
    public static byte[] readToBytes(File inFile) throws IOException
    {
        RandomAccessFile randomAccessFile = new RandomAccessFile(inFile, "r");
        try
        {
            byte[] fileBytes = new byte[(int) randomAccessFile.length()];
            randomAccessFile.readFully(fileBytes);
            return fileBytes;
        }
        finally
        {
            close(randomAccessFile);
        }
    }

    /**
     * closes a Closeable, and silently ignores exceptions
     *
     * @param closeable the closeable to close
     */
    public static void close(Closeable closeable)
    {
        if(closeable == null)
            return;

        try
        {
            closeable.close();
        }
        catch(IOException ignore)
        {
            /* ignore */
        }
    }

    public static int[] readFile(String fileName) throws IOException
    {
        String fileContents = readToString(new File(fileName));
        return toIntArray(fileContents);
    }

    public static int[] toIntArray(String inString)
    {
        int[] intArray = new int[inString.length()];
        for (int i = 0; i < inString.length(); i++)
        {
            intArray[i] = inString.charAt(i);
        }
        return intArray;
    }

    public static String unGzip(byte[] compressedByteArray) throws IOException
    {
        GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream (compressedByteArray));
        StringWriter stringWriter = new StringWriter();
        InputStreamReader inputReader = new InputStreamReader (gzipInput, StandardCharsets.UTF_8);
        copy (inputReader, stringWriter);
        return stringWriter.toString();
    }

    public static String nativeCall (final String... commands) throws IOException
    {
        final ProcessBuilder processBuilder = new ProcessBuilder (commands);
        final Process runProcess = processBuilder.start ();
        final InputStream inStream = runProcess.getInputStream ();
        return new String (readToBytes (inStream));
    }

    private static HashMap <String, String> readResponseHeaders (HttpURLConnection urlConn)
    {
        HashMap <String, String> responseHeaders = new HashMap <> ();
        for (int i = 0; ; i++)
        {
            String headerName = urlConn.getHeaderFieldKey (i);
            String headerValue = urlConn.getHeaderField (i);
            responseHeaders.put (headerName, headerValue);
            if (headerName == null && headerValue == null)
                break;
        }
        return responseHeaders;
    }

    public static HttpResponse getUrl (String urlString, HashMap <String, String> requestHeaders) throws Exception
    {
        URL url = new URL (urlString);

        // Connect to your end point by passing the previously created proxy to the connection
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection ();
        if (requestHeaders != null)
            for (Map.Entry <String, String> currHeader : requestHeaders.entrySet ())
                urlConn.setRequestProperty (currHeader.getKey (), currHeader.getValue ());

        int responseCode = urlConn.getResponseCode();
        int responseLength = urlConn.getContentLength();
        String contentType = urlConn.getContentType();

        InputStream inputStream;
        if(responseCode < 300)
            inputStream = urlConn.getInputStream();
        else
            inputStream = urlConn.getErrorStream();
        HashMap <String, String> responseHeaders = readResponseHeaders (urlConn);

        byte[] resultBuffer = readToBytes (inputStream);

        inputStream.close();
        urlConn.disconnect();
        return new HttpResponse (responseCode, responseLength, responseHeaders, contentType, resultBuffer);
    }

    public static HttpResponse postUrl (String urlString, HashMap <String, String> postParams, HashMap <String, String> requestHeaders) throws Exception
    {
        URL url = new URL (urlString);

        StringBuilder postData = new StringBuilder ();
        for (Map.Entry <String, String> param : postParams.entrySet ())
        {
            if (postData.length () != 0)
            {
                postData.append ('&');
            }
            postData.append (URLEncoder.encode (param.getKey (), "UTF-8"));
            postData.append ('=');
            postData.append (URLEncoder.encode (String.valueOf (param.getValue ()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString ().getBytes (StandardCharsets.UTF_8);

        // Connect to your end point by passing the previously created proxy to the connection
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection ();
        urlConn.setRequestMethod ("POST");
        urlConn.setRequestProperty ("Content-Length", String.valueOf (postDataBytes.length));

        if (requestHeaders != null)
            for (Map.Entry <String, String> currHeader : requestHeaders.entrySet ())
                urlConn.setRequestProperty (currHeader.getKey (), currHeader.getValue ());

        urlConn.setDoOutput (true);
        urlConn.getOutputStream ().write (postDataBytes);

        int responseCode = urlConn.getResponseCode();
        int responseLength = urlConn.getContentLength();
        String contentType = urlConn.getContentType();

        InputStream inputStream;
        if(responseCode < 300)
            inputStream = urlConn.getInputStream();
        else
            inputStream = urlConn.getErrorStream();
        HashMap <String, String> responseHeaders = readResponseHeaders (urlConn);
        byte[] resultBuffer = readToBytes (inputStream);
        inputStream.close();
        urlConn.disconnect();
        return new HttpResponse (responseCode, responseLength, responseHeaders, contentType, resultBuffer);
    }

    public static byte[] readStream (InputStream is) throws IOException
    {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream ();
        while ((bytesRead = is.read (buffer)) != -1)
        {
            output.write (buffer, 0, bytesRead);
        }

        return output.toByteArray ();
    }

    public static class HttpResponse
    {
        public int responseCode;
        public int responseLength;
        HashMap<String, String> responseHeaders;
        public String contentType;
        public byte[] resultBuffer;

        public HttpResponse(int responseCode, int responseLength, HashMap<String, String> responseHeaders, String contentType, byte[] resultBuffer)
        {
            this.responseCode = responseCode;
            this.responseLength = responseLength;
            this.responseHeaders = responseHeaders;
            this.contentType = contentType;
            this.resultBuffer = resultBuffer;
        }

        public String getResponseString()
        {
            return new String (resultBuffer);
        }
    }
}
