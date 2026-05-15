import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestAuth {
    public static void main(String[] args) throws Exception {
        String json = "{\"fullName\":\"Test User\",\"email\":\"test3@stockpro.com\",\"password\":\"Password@123!\",\"phone\":\"1234567890\",\"role\":\"WAREHOUSE_STAFF\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8083/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
