package co.wethinkcode.trafficflow;

public final class Services {

    public static final ServiceEndpoint INGESTION = new ServiceEndpoint("ingestion", 7020);
    public static final ServiceEndpoint INTERSECTION = new ServiceEndpoint("intersection", 7021);
    public static final ServiceEndpoint CONGESTION = new ServiceEndpoint("congestion", 7022);
    public static final ServiceEndpoint ROUTING = new ServiceEndpoint("routing", 7023);
    public static final ServiceEndpoint WATCHDOG = new ServiceEndpoint("watchdog", 7024);

    private Services() {
    }
}
