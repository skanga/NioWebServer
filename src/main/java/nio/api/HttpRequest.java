package nio.api;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest
{
    private String httpMethod;
    private String reqLocation;
    private String httpVersion;
    private final Map <String, String> reqHeaders = new HashMap <> ();
    private final Map <String, String> postParams = new HashMap <> ();

    public HttpRequest (String rawRequest)
    {
        parseReq (rawRequest);
    }

    private void parseReq (String rawRequest)
    {
        String[] allLines = rawRequest.split ("\r\n");
        // parse the first line
        String[] firstLine = allLines[0].split (" ");
        httpMethod = firstLine[0].toUpperCase ();
        reqLocation = firstLine[1];
        httpVersion = firstLine[2];
        // parse the headers
        for (int i = 1; i < allLines.length; i++)
        {
            allLines[i] = allLines[i].trim ();
            if (allLines[i].equals ("") && !allLines[i + 1].isEmpty () && httpMethod.equals ("POST"))
            {
                parsePostParams (allLines[i + 1]);
                break;
            }
            String[] keyVal = allLines[i].split (":", 2);
            reqHeaders.put (keyVal[0], keyVal[1]);
        }
    }

    private void parsePostParams(String postParamLine)
    {
        // parse the post params
        String[] postParams = postParamLine.split ("&");
        for (String currParam : postParams)
        {
            String[] keyValue = currParam.split ("=");
            this.postParams.put (keyValue[0], keyValue[1]);
        }
    }

    public String getHttpMethod ()
    {
        return httpMethod;
    }

    public String getHttpVersion ()
    {
        return httpVersion;
    }

    public String getReqLocation ()
    {
        return reqLocation;
    }

    public String getHeader (String key)
    {
        return reqHeaders.get (key);
    }

    public Map <String, String> getPostParams ()
    {
        return postParams;
    }

    public String getAppName ()
    {
        String appName = reqLocation;
        if (appName.startsWith ("/"))
            appName = appName.substring (1);
        if (appName.contains ("/"))
            appName = appName.substring (0, appName.indexOf ("/"));
        if (appName.contains ("?"))
            appName = appName.substring (0, appName.indexOf ("?"));
        return appName;
    }
}
