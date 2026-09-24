package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class IntersectionWatchdogApp {

    public record WatchdogStatus(String service, boolean healthy, Instant lastHeartbeat, String status) {
    }

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 5;
    private static final AtomicReference<Instant> lastHeartbeat = new AtomicReference<>(null);

    public static void main(String[] args) {
        subscribeToHeartbeatQueue();

        Javalin app = Javalin.create().start(Services.WATCHDOG.port());
        app.get("/health", ctx -> {
            if (isHealthy()) {
                ctx.result("OK");
            } else {
                ctx.status(503).result("ALERT: Intersection service heartbeat missing");
            }
        });

        app.get("/status", ctx -> {
            boolean healthy = isHealthy();
            ctx.json(new WatchdogStatus("intersection-service", healthy, lastHeartbeat.get(), healthy ? "OK" : "DOWN"));
        });
    }

    private static boolean isHealthy() {
        Instant last = lastHeartbeat.get();
        if (last == null) {
            return false;
        }

        long timeSinceLast = Duration.between(last, Instant.now()).getSeconds();

        return timeSinceLast <= HEARTBEAT_TIMEOUT_SECONDS;
    }

    private static void subscribeToHeartbeatQueue() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            MessageConsumer consumer = session.createConsumer(queue);

            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    lastHeartbeat.set(Instant.now());
                }
            });
        } catch (Exception e) {
            System.err.println("ActiveMQ broker unavailable for watchdog subscriber: " + e.getMessage());
        }
    }
}

// MQ TODO: subscribes to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at MqConfig.BROKER_URL
// (see co.wethinkcode.trafficflow.mq.MqConfig) and alerts if a heartbeat from
// intersection-service is missed or a message lands in the dead-letter queue.
