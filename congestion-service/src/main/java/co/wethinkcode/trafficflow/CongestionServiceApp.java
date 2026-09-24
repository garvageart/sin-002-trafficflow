package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class CongestionServiceApp {

    public record Congestion(int level) {}

    private static volatile Congestion current = new Congestion(0);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Session session;
    private static MessageProducer producer;

    public static void main(String[] args) {
        initMq();

        Javalin app = Javalin.create().start(Services.CONGESTION.port());

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/congestion", ctx -> ctx.json(current));

        app.post("/congestion", ctx -> {
            Congestion updatedVal = ctx.bodyAsClass(Congestion.class);
            if (updatedVal.level() >= 0 && updatedVal.level() <= 8) {
                current = updatedVal;

                publishCongestionUpdate(current);
                ctx.json(current);
            } else {
                ctx.status(400).result("Invalid congestion level. Must be between 0 and 8.");
            }
        });
    }

    private static void initMq() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            producer = session.createProducer(topic);
        } catch (Exception e) {
            System.err.println("ActiveMQ broker unavailable at startup: " + e.getMessage());
        }
    }

    private static void publishCongestionUpdate(Congestion congestion) {
        if (producer == null) {
            return;
        }

        try {
            String payload = mapper.writeValueAsString(congestion);
            TextMessage message = session.createTextMessage(payload);
            producer.send(message);
        } catch (Exception e) {
            System.err.println("Failed to publish congestion update to ActiveMQ: " + e.getMessage());
        }
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
