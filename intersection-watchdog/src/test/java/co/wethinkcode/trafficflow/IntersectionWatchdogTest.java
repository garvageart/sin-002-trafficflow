package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Intersection Watchdog Tests")
class IntersectionWatchdogTest {

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 5;

    private boolean isHealthy(Instant lastHeartbeat, Instant checkTime) {
        if (lastHeartbeat == null) {
            return false;
        }
        return Duration.between(lastHeartbeat, checkTime).getSeconds() <= HEARTBEAT_TIMEOUT_SECONDS;
    }

    @Nested
    @DisplayName("WatchdogStatus Record Model Tests")
    class WatchdogStatusModelTests {

        @Test
        @DisplayName("WatchdogStatus stores health state, timestamp, and status string")
        void recordStoresStatusCorrectly() {
            Instant now = Instant.now();
            IntersectionWatchdogApp.WatchdogStatus status = new IntersectionWatchdogApp.WatchdogStatus(
                    "intersection-service",
                    true,
                    now,
                    "OK"
            );

            assertThat(status.service()).isEqualTo("intersection-service");
            assertThat(status.healthy()).isTrue();
            assertThat(status.lastHeartbeat()).isEqualTo(now);
            assertThat(status.status()).isEqualTo("OK");
        }

        @Test
        @DisplayName("WatchdogStatus handles null lastHeartbeat when down")
        void handlesNullHeartbeatWhenDown() {
            IntersectionWatchdogApp.WatchdogStatus status = new IntersectionWatchdogApp.WatchdogStatus(
                    "intersection-service",
                    false,
                    null,
                    "DOWN"
            );

            assertThat(status.healthy()).isFalse();
            assertThat(status.lastHeartbeat()).isNull();
            assertThat(status.status()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Watchdog Timeout and Failure Alert Tests")
    class TimeoutAlertTests {

        @Test
        @DisplayName("Returns unhealthy when no heartbeat has ever been received")
        void unhealthyWhenNeverReceived() {
            assertThat(isHealthy(null, Instant.now())).isFalse();
        }

        @ParameterizedTest(name = "Heartbeat received {0} seconds ago is healthy")
        @ValueSource(longs = {0, 1, 2, 3, 4, 5})
        void healthyWithinTimeoutWindow(long secondsAgo) {
            Instant now = Instant.now();
            Instant heartbeatTime = now.minusSeconds(secondsAgo);

            assertThat(isHealthy(heartbeatTime, now)).isTrue();
        }

        @ParameterizedTest(name = "Heartbeat received {0} seconds ago triggers alert (unhealthy)")
        @ValueSource(longs = {6, 7, 10, 30, 60, 3600})
        void unhealthyExceedingTimeoutWindow(long secondsAgo) {
            Instant now = Instant.now();
            Instant heartbeatTime = now.minusSeconds(secondsAgo);

            assertThat(isHealthy(heartbeatTime, now)).isFalse();
        }

        @Test
        @DisplayName("Boundary at exactly 5 seconds is healthy, 6 seconds is unhealthy")
        void boundaryTest() {
            Instant now = Instant.now();
            assertThat(isHealthy(now.minusSeconds(5), now)).isTrue();
            assertThat(isHealthy(now.minusSeconds(6), now)).isFalse();
        }
    }
}
