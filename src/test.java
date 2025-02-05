import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RssFetchLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, String> queryParams = (Map<String, String>) event.get("queryStringParameters");
            if (queryParams == null || !queryParams.containsKey("url")) {
                response.put("statusCode", 400);
                response.put("body", objectMapper.writeValueAsString(Map.of("error", "URL parameter is required")));
                return response;
            }
            
            String url = queryParams.get("url");
            List<Map<String, String>> items = fetchRss(url);
            
            response.put("statusCode", 200);
            response.put("body", objectMapper.writeValueAsString(items));
            response.put("headers", Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", objectMapper.writeValueAsString(Map.of("error", "Failed to fetch RSS", "details", e.getMessage())));
        }
        
        return response;
    }
    
    private List<Map<String, String>> fetchRss(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        InputStream inputStream = conn.getInputStream();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        
        NodeList itemNodes = doc.getElementsByTagName("item");
        List<Map<String, String>> items = new ArrayList<>();
        
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Node itemNode = itemNodes.item(i);
            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) itemNode;
                Map<String, String> item = new HashMap<>();
                item.put("title", getElementText(element, "title"));
                item.put("link", getElementText(element, "link"));
                item.put("description", getElementText(element, "description"));
                item.put("pubDate", getElementText(element, "pubDate"));
                items.add(item);
            }
        }
        return items;
    }
    
    private String getElementText(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        return (nodeList.getLength() > 0) ? nodeList.item(0).getTextContent() : "";
    }
}
