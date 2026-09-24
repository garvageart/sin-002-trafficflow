package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        IntersectionCsvCleaner cleaner = new IntersectionCsvCleaner();
        List<Intersection> intersections = cleaner.cleanFromClasspath("intersections-legacy.csv");

        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/intersections", ctx -> ctx.json(intersections));
    }
}
