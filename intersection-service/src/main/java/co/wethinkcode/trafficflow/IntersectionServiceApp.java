package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntersectionServiceApp {

    public record Heartbeat(String service, String status, String timestamp) {}

    private static final Map<String, Intersection> intersections = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static Session heartbeatSession;
    private static MessageProducer heartbeatProducer;

    public static void main(String[] args) {
        loadIntersections();
        initMq();
        startHeartbeatSender();

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

    private static void initMq() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            heartbeatSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = heartbeatSession.createQueue(MqConfig.HEARTBEAT_QUEUE);
            heartbeatProducer = heartbeatSession.createProducer(queue);
        } catch (Exception e) {
            System.err.println("ActiveMQ broker unavailable at startup for heartbeat sender: " + e.getMessage());
        }
    }

    private static void startHeartbeatSender() {
        if (heartbeatProducer == null) {
            return;
        }
        scheduler.scheduleAtFixedRate(IntersectionServiceApp::sendHeartbeat, 1, 2, TimeUnit.SECONDS);
    }

    private static void sendHeartbeat() {
        try {
            Heartbeat heartbeat = new Heartbeat("intersection-service", "ALIVE", Instant.now().toString());
            String payload = mapper.writeValueAsString(heartbeat);
            TextMessage message = heartbeatSession.createTextMessage(payload);
            heartbeatProducer.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send heartbeat to ActiveMQ: " + e.getMessage());
        }
    }
}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
