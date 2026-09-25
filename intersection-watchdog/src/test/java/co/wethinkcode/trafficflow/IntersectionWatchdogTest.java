package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Intersection Watchdog Tests")
class IntersectionWatchdogTest {

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
}
