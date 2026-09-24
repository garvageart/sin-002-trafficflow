package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IntersectionServiceApp {

    private static final Map<String, Intersection> intersections = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        loadIntersections();

        Javalin app = Javalin.create().start(Services.INTERSECTION.port());
        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/intersections", ctx -> {
            if (intersections.isEmpty()) {
                loadIntersections();
            }
            ctx.json(intersections.values());
        });

        app.get("/intersections/{id}", ctx -> {
            String id = ctx.pathParam("id").toUpperCase();
            if (intersections.isEmpty()) {
                loadIntersections();
            }

            Intersection record = intersections.get(id);
            if (record != null) {
                ctx.json(record);
            } else {
                ctx.status(404).result("Intersection not found: " + id);
            }
        });
    }

    private static void loadIntersections() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Services.INGESTION.url("/intersections")))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Intersection> list = mapper.readValue(response.body(), new TypeReference<List<Intersection>>() {});
                for (Intersection item : list) {
                    intersections.put(item.id().toUpperCase(), item);
                }
            } else {
                System.err.println("Ingestion service returned status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Failed to load intersections from Ingestion Service: " + e.getMessage());
        }
    }
}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
