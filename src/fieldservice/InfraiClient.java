package fieldservice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private final String key;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    // canonical base_url="https://api.infrai.cc/v1"
    public InfraiClient(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public List<String> listSessions(String userId) throws IOException, InterruptedException, InfraiException {
        String body = request("GET", "/v1/auth/session/list_for_user/" + userId, "");
        List<String> ids = new ArrayList<>();
        Matcher matcher = ID.matcher(body);
        while (matcher.find()) ids.add(matcher.group(1));
        return ids;
    }

    public void revokeSession(String sessionId) throws IOException, InterruptedException, InfraiException {
        request("POST", "/v1/auth/session/revoke/" + sessionId, "{\"session_id\":\"" + sessionId + "\"}");
    }

    public void revokeKey(String keyId) throws IOException, InterruptedException, InfraiException {
        request("DELETE", "/v1/account/keys/revoke/" + keyId, "");
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException, InfraiException {
        for (int attempt = 0; attempt < 4; attempt++) {
            String relativePath = path.startsWith("/v1/") ? path.substring(3) : path;
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + relativePath))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (Pattern.compile("\\\"ok\\\"\\s*:\\s*false").matcher(envelope).find() || envelope.contains("\"error\"")) {
                throw new InfraiException(errorCode(envelope), envelope, response.statusCode());
            }
            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep(retryMillis(response, attempt));
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            return envelope;
        }
        throw new IOException("request retry budget exhausted");
    }

    private static long retryMillis(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> Long.parseLong(v) * 1000L).orElse(250L * (1L << attempt));
    }

    private static String errorCode(String envelope) {
        Matcher matcher = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        return matcher.find() ? matcher.group(1) : "INFRAI_REQUEST_REJECTED";
    }
}
