package co.wethinkcode.trafficflow;

public record ServiceEndpoint(String name, int port, String host) {

    public static final String DEFAULT_HOST = "localhost";

    public ServiceEndpoint(String name, int port) {
        this(name, port, System.getenv().getOrDefault(name.toUpperCase() + "_HOST", DEFAULT_HOST));
    }

    public String url() {
        return "http://" + host + ":" + port;
    }

    public String url(String path) {
        if (path == null || path.isEmpty()) {
            return url();
        }
        return url() + (path.startsWith("/") ? path : "/" + path);
    }
}
