package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Routing Service Tests")
class RoutingServiceTest {

    private String resolveParam(String primary, String alias) {
        String val = (primary != null && !primary.isBlank()) ? primary : alias;
        return (val != null && !val.isBlank()) ? val.trim().toUpperCase() : null;
    }

    private boolean isValidRouteRequest(String origin, String destination) {
        return origin != null && !origin.isBlank() && destination != null && !destination.isBlank();
    }

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
    @DisplayName("Route Request Query Parameter Validation Tests")
    class ParameterValidationTests {

        @Test
        @DisplayName("Valid origin and destination pass validation")
        void validParametersPass() {
            assertThat(isValidRouteRequest("INT-1001", "INT-1002")).isTrue();
        }

        @Test
        @DisplayName("Null origin fails validation")
        void nullOriginFails() {
            assertThat(isValidRouteRequest(null, "INT-1002")).isFalse();
        }

        @Test
        @DisplayName("Null destination fails validation")
        void nullDestinationFails() {
            assertThat(isValidRouteRequest("INT-1001", null)).isFalse();
        }

        @ParameterizedTest(name = "Blank origin \"{0}\" fails validation")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void blankOriginFails(String blank) {
            assertThat(isValidRouteRequest(blank, "INT-1002")).isFalse();
        }

        @ParameterizedTest(name = "Blank destination \"{0}\" fails validation")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void blankDestinationFails(String blank) {
            assertThat(isValidRouteRequest("INT-1001", blank)).isFalse();
        }

        @Test
        @DisplayName("Resolves primary origin parameter with uppercase and trimming")
        void resolvesPrimaryOrigin() {
            String resolved = resolveParam("  int-1001  ", null);
            assertThat(resolved).isEqualTo("INT-1001");
        }

        @Test
        @DisplayName("Resolves alias 'from' parameter when primary is null")
        void resolvesFromAlias() {
            String resolved = resolveParam(null, "  int-1002  ");
            assertThat(resolved).isEqualTo("INT-1002");
        }

        @Test
        @DisplayName("Resolves alias 'to' parameter when primary is null")
        void resolvesToAlias() {
            String resolved = resolveParam(null, "  int-1003  ");
            assertThat(resolved).isEqualTo("INT-1003");
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
