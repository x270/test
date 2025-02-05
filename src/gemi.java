import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedInput;
import com.rometools.rome.feed.synd.SyndEntry;
import java.net.URL;
import java.io.StringReader;
import java.util.List;

public class RssHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // リクエストパラメータからURLを取得
            String url = request.getQueryStringParameters().get("url");

            // URLが存在しない場合はエラー
            if (url == null || url.isEmpty()) {
                response.setStatusCode(400);
                response.setBody(objectMapper.writeValueAsString(new ErrorResponse("URL is required")));
                return response;
            }

            // RSSフィードを取得
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new StringReader(getResponseBody(url)));

            // item要素をJSONに変換
            ArrayNode items = objectMapper.createArrayNode();
            List<SyndEntry> entries = feed.getEntries();
            for (SyndEntry entry : entries) {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("title", entry.getTitle());
                item.put("link", entry.getLink());
                item.put("description", entry.getDescription().getValue());
                items.add(item);
            }

            // JSONをレスポンスボディに設定
            response.setStatusCode(200);
            response.setBody(items.toString());

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody(objectMapper.writeValueAsString(new ErrorResponse("Internal Server Error: " + e.getMessage())));
            e.printStackTrace(); // エラーログを出力
        }

        return response;
    }

    // URLからHTTPレスポンスボディを取得するメソッド
    private String getResponseBody(String url) throws Exception {
        // ここにHTTPクライアントの実装 (例: java.net.http.HttpClient) を記述
        // ...
    }

    // エラーレスポンス用クラス
    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
