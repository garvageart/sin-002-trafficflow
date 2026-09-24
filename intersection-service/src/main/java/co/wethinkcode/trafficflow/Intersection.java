package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record Intersection(
    String id,
    String district,
    String signalType,
    Boolean active
) {}
