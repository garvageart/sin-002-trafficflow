package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Routing Service Tests")
class RoutingServiceTest {

    @Nested
    @DisplayName("Travel Time Calculation Formula Tests")
    class TravelTimeFormulaTests {

        @ParameterizedTest(name = "Congestion level {0} produces estimated time of {1} minutes")
        @CsvSource({
                "0, 10.0",
                "1, 12.5",
                "2, 15.0",
                "3, 17.5",
                "4, 20.0",
                "5, 22.5",
                "6, 25.0",
                "7, 27.5",
                "8, 30.0"
        })
        void calculatesTravelTimeAcrossAllCongestionLevels(int level, double expectedMinutes) {
            double actual = RoutingServiceApp.calculateTravelTime(level);
            assertThat(actual).isCloseTo(expectedMinutes, within(0.001));
        }

        @Test
        @DisplayName("Zero congestion level equals base travel time (10.0 minutes)")
        void zeroCongestionIsBaseTravelTime() {
            double actual = RoutingServiceApp.calculateTravelTime(0);
            assertThat(actual).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Maximum congestion level 8 triples the base travel time (30.0 minutes)")
        void maxCongestionTriplesBaseTime() {
            double actual = RoutingServiceApp.calculateTravelTime(8);
            assertThat(actual).isEqualTo(30.0);
        }
    }

    @Nested
    @DisplayName("RouteEstimate Record Model Tests")
    class RouteEstimateModelTests {

        @Test
        @DisplayName("RouteEstimate record correctly preserves origin, destination, level, and time")
        void recordPreservesProperties() {
            RoutingServiceApp.RouteEstimate estimate = new RoutingServiceApp.RouteEstimate(
                    "INT-1001",
                    "INT-1002",
                    3,
                    17.5
            );

            assertThat(estimate.origin()).isEqualTo("INT-1001");
            assertThat(estimate.destination()).isEqualTo("INT-1002");
            assertThat(estimate.congestionLevel()).isEqualTo(3);
            assertThat(estimate.estimatedTimeMinutes()).isEqualTo(17.5);
        }

        @Test
        @DisplayName("RouteEstimate equals and hashCode work as expected")
        void recordEqualsAndHashCode() {
            RoutingServiceApp.RouteEstimate first = new RoutingServiceApp.RouteEstimate(
                    "INT-1001", "INT-1002", 2, 15.0
            );
            RoutingServiceApp.RouteEstimate second = new RoutingServiceApp.RouteEstimate(
                    "INT-1001", "INT-1002", 2, 15.0
            );

            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }
    }
}
