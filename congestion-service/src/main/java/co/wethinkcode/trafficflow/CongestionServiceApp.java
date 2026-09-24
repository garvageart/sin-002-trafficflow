package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

public class CongestionServiceApp {

    public record Congestion(int level) {}

    private static volatile Congestion current = new Congestion(0);

    public static void main(String[] args) {
        try (Javalin app = Javalin.create().start(Services.CONGESTION.port())) {
            app.get("/health", ctx -> ctx.result("OK"));

            app.get("/congestion", ctx -> ctx.json(current));

            app.post("/congestion", ctx -> {
                Congestion updatedVal = ctx.bodyAsClass(Congestion.class);
                if (updatedVal.level() >= 0 && updatedVal.level() <= 8) {
                    current = updatedVal;
                    ctx.json(current);
                } else {
                    ctx.status(400).result("Invalid congestion level. Must be between 0 and 8.");
                }
            });

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
