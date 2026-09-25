package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Intersection Service Tests")
class IntersectionServiceTest {

    @Nested
    @DisplayName("Intersection Record Model Tests")
    class IntersectionModelTests {

        @Test
        @DisplayName("Intersection record stores fields accurately")
        void recordStoresFields() {
            Intersection item = new Intersection("INT-1001", "Downtown", "4-way", true);

            assertThat(item.id()).isEqualTo("INT-1001");
            assertThat(item.district()).isEqualTo("Downtown");
            assertThat(item.signalType()).isEqualTo("4-way");
            assertThat(item.active()).isTrue();
        }

        @Test
        @DisplayName("Intersection handles null fields gracefully")
        void recordHandlesNullFields() {
            Intersection item = new Intersection("INT-1002", null, null, null);

            assertThat(item.id()).isEqualTo("INT-1002");
            assertThat(item.district()).isNull();
            assertThat(item.signalType()).isNull();
            assertThat(item.active()).isNull();
        }

        @Test
        @DisplayName("Heartbeat record correctly stores service status and timestamp")
        void heartbeatRecordStoresValues() {
            IntersectionServiceApp.Heartbeat heartbeat = new IntersectionServiceApp.Heartbeat(
                    "intersection-service", "ALIVE", "2026-09-25T00:00:00Z"
            );

            assertThat(heartbeat.service()).isEqualTo("intersection-service");
            assertThat(heartbeat.status()).isEqualTo("ALIVE");
            assertThat(heartbeat.timestamp()).isEqualTo("2026-09-25T00:00:00Z");
        }
    }
}
