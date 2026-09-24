package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        IntersectionCsvCleaner cleaner = new IntersectionCsvCleaner();
        List<Intersection> intersections = cleaner.cleanFromClasspath("intersections-legacy.csv");

        try (Javalin app = Javalin.create().start(Services.INGESTION.port())) {
            app.get("/health", ctx -> ctx.result("OK"));
            app.get("/intersections", ctx -> ctx.json(intersections));

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
