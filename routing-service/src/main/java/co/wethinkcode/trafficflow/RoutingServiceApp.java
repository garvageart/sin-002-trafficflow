package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;

public class RoutingServiceApp {

    public record RouteEstimate(String origin, String destination, int congestionLevel, double estimatedTimeMinutes) {}

    private static final double BASE_TRAVEL_TIME_MINUTES = 10.0;
    private static final double CONGESTION_MULTIPLIER = 0.25;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AtomicInteger currentCongestionLevel = new AtomicInteger(0);

    public static void main(String[] args) {
        subscribeToCongestionTopic();

        Javalin app = Javalin.create().start(Services.ROUTING.port());
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

            int level = currentCongestionLevel.get();
            double estimatedMinutes = calculateTravelTime(level);
            ctx.json(new RouteEstimate(origin, destination, level, estimatedMinutes));
        });
    }

    private static void subscribeToCongestionTopic() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);

            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage textMessage) {
                    try {
                        JsonNode node = mapper.readTree(textMessage.getText());
                        if (node.has("level")) {
                            currentCongestionLevel.set(node.get("level").asInt());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse congestion update message: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("ActiveMQ broker unavailable for congestion subscriber: " + e.getMessage());
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
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
