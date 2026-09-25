package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Congestion Service Tests")
class CongestionServiceTest {

    @Nested
    @DisplayName("Congestion Record DTO Tests")
    class CongestionModelTests {

        @ParameterizedTest(name = "Congestion record accepts valid level {0}")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
        void recordAcceptsValidLevels(int level) {
            CongestionServiceApp.Congestion congestion = new CongestionServiceApp.Congestion(level);
            assertThat(congestion.level()).isEqualTo(level);
        }

        @Test
        @DisplayName("Congestion equals and hashCode work correctly")
        void recordEqualsAndHashCode() {
            CongestionServiceApp.Congestion first = new CongestionServiceApp.Congestion(4);
            CongestionServiceApp.Congestion second = new CongestionServiceApp.Congestion(4);
            CongestionServiceApp.Congestion third = new CongestionServiceApp.Congestion(5);

            assertThat(first).isEqualTo(second);
            assertThat(first).isNotEqualTo(third);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }
    }
}
