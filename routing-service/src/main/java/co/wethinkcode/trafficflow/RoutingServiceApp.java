package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RoutingServiceApp {

    public record RouteEstimate(String origin, String destination, int congestionLevel, double estimatedTimeMinutes) {}

    private static final double BASE_TRAVEL_TIME_MINUTES = 10.0;
    private static final double CONGESTION_MULTIPLIER = 0.25;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try (Javalin app = Javalin.create().start(Services.ROUTING.port())) {
            app.get("/health", ctx -> ctx.result("OK"));

            app.get("/route", ctx -> {
                String origin = ctx.queryParam("origin") != null ? ctx.queryParam("origin") : ctx.queryParam("from");
                String destination = ctx.queryParam("destination") != null ? ctx.queryParam("destination") : ctx.queryParam("to");

                if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
                    ctx.status(400).result("Missing required query parameters: 'origin' and 'destination'.");
                    return;
                }

                origin = origin.trim().toUpperCase();
                destination = destination.trim().toUpperCase();

                Boolean originValid = validateIntersection(origin);
                if (originValid == null) {
                    ctx.status(503).result("Intersection service unavailable.");
                    return;
                }
                if (!originValid) {
                    ctx.status(404).result("Invalid origin intersection: " + origin);
                    return;
                }

                Boolean destinationValid = validateIntersection(destination);
                if (destinationValid == null) {
                    ctx.status(503).result("Intersection service unavailable.");
                    return;
                }
                if (!destinationValid) {
                    ctx.status(404).result("Invalid destination intersection: " + destination);
                    return;
                }

                Integer level = fetchCongestionLevel();
                if (level == null) {
                    ctx.status(503).result("Congestion service unavailable.");
                    return;
                }

                double estimatedMinutes = calculateTravelTime(level);
                ctx.json(new RouteEstimate(origin, destination, level, estimatedMinutes));
            });

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static double calculateTravelTime(int congestionLevel) {
        return BASE_TRAVEL_TIME_MINUTES * (1.0 + (congestionLevel * CONGESTION_MULTIPLIER));
    }

    private static Boolean validateIntersection(String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Services.INTERSECTION.url("/intersections/" + id)))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return true;
            }

            if (response.statusCode() == 404) {
                return false;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // LESTODO: Create a `trafficflow` client to fetch data from a service instead of writing
    // repetitive code
    private static Integer fetchCongestionLevel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Services.CONGESTION.url("/congestion")))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = mapper.readTree(response.body());
                if (node.has("level")) {
                    return node.get("level").asInt();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
