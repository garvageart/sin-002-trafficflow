package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Congestion Service Tests")
class CongestionServiceTest {

    private boolean isValidCongestionLevel(int level) {
        return level >= 0 && level <= 8;
    }

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

    @Nested
    @DisplayName("Congestion Level Validation and Boundary Tests")
    class CongestionValidationTests {

        @ParameterizedTest(name = "Level {0} is valid (within 0..8)")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
        void validLevelsPassValidation(int level) {
            assertThat(isValidCongestionLevel(level)).isTrue();
        }

        @ParameterizedTest(name = "Level {0} is invalid (outside 0..8)")
        @ValueSource(ints = {-100, -5, -1, 9, 10, 50, 100})
        void outOfRangeLevelsFailValidation(int level) {
            assertThat(isValidCongestionLevel(level)).isFalse();
        }

        @Test
        @DisplayName("Boundary levels 0 and 8 are valid")
        void boundaryLevelsAreValid() {
            assertThat(isValidCongestionLevel(0)).isTrue();
            assertThat(isValidCongestionLevel(8)).isTrue();
        }

        @Test
        @DisplayName("Just outside boundary levels -1 and 9 are invalid")
        void adjacentBoundaryLevelsAreInvalid() {
            assertThat(isValidCongestionLevel(-1)).isFalse();
            assertThat(isValidCongestionLevel(9)).isFalse();
        }
    }
}
