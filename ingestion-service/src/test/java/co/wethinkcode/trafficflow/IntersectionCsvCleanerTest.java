package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IntersectionCsvCleaner Tests")
class IntersectionCsvCleanerTest {

    private IntersectionCsvCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new IntersectionCsvCleaner();
    }

    private ByteArrayInputStream createCsvStream(String... lines) {
        String csv = String.join("\n", lines);
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("Classpath and Resource Loading")
    class ResourceLoadingTests {

        @Test
        @DisplayName("cleanFromClasspath successfully cleans legacy dataset")
        void cleansLegacyDatasetFromClasspath() {
            List<Intersection> result = cleaner.cleanFromClasspath("intersections-legacy.csv");

            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(17);

            Intersection first = result.get(0);
            assertThat(first.id()).isEqualTo("INT-1001");
            assertThat(first.district()).isEqualTo("Downtown");
            assertThat(first.signalType()).isEqualTo("4-way");
            assertThat(first.active()).isTrue();
        }

        @Test
        @DisplayName("cleanFromClasspath throws on missing resource")
        void throwsOnMissingResource() {
            assertThatThrownBy(() -> cleaner.cleanFromClasspath("non-existent-file.csv"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resource not found");
        }
    }

    @Nested
    @DisplayName("CSV Parsing Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Returns empty list when CSV is completely empty")
        void returnsEmptyListForEmptyInput() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(""));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns empty list when CSV contains only header")
        void returnsEmptyListForHeaderOnly() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag"
            ));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Skips rows with missing or placeholder IDs")
        void skipsRowsWithInvalidId() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    ",Downtown,4-way,Y",
                    "n/a,Downtown,4-way,Y",
                    "-,Midtown,pedestrian,1",
                    "INT-2001,Uptown,roundabout,true"
            ));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("INT-2001");
        }
    }

    @Nested
    @DisplayName("Casing and Whitespace Normalization")
    class NormalizationTests {

        @Test
        @DisplayName("Normalizes ID to uppercase and trims padding")
        void normalizesId() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "  int-9999  ,Downtown,4-way,Y"
            ));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("INT-9999");
        }

        @Test
        @DisplayName("Normalizes District to title case and trims padding")
        void normalizesDistrict() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1, downtown ,4-way,Y",
                    "INT-2,UPTOWN,4-way,Y",
                    "INT-3,eAsTsIdE,4-way,Y"
            ));

            assertThat(result)
                    .extracting(Intersection::district)
                    .containsExactly("Downtown", "Uptown", "Eastside");
        }

        @Test
        @DisplayName("Normalizes Signal Type to lowercase and trims padding")
        void normalizesSignalType() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1,Downtown,  ROUNDABOUT  ,Y",
                    "INT-2,Downtown,4-Way,Y",
                    "INT-3,Downtown,STOP-SIGN,Y"
            ));

            assertThat(result)
                    .extracting(Intersection::signalType)
                    .containsExactly("roundabout", "4-way", "stop-sign");
        }
    }

    @Nested
    @DisplayName("Active Flag Normalization")
    class ActiveFlagTests {

        @ParameterizedTest(name = "Normalizes \"{0}\" to TRUE")
        @ValueSource(strings = {"Y", "y", "yes", "YES", "true", "TRUE", "1"})
        void normalizesTruthyValues(String flag) throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1,Downtown,4-way," + flag
            ));

            assertThat(result.get(0).active()).isTrue();
        }

        @ParameterizedTest(name = "Normalizes \"{0}\" to FALSE")
        @ValueSource(strings = {"N", "n", "no", "NO", "false", "FALSE", "0"})
        void normalizesFalsyValues(String flag) throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1,Downtown,4-way," + flag
            ));

            assertThat(result.get(0).active()).isFalse();
        }

        @ParameterizedTest(name = "Normalizes \"{0}\" to NULL")
        @ValueSource(strings = {"", "n/a", "na", "unknown", "tbd", "-", "nan", "null"})
        void normalizesPlaceholderActiveFlagsToNull(String flag) throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1,Downtown,4-way," + flag
            ));

            assertThat(result.get(0).active()).isNull();
        }
    }

    @Nested
    @DisplayName("Placeholder Values and Deduplication")
    class PlaceholderAndDeduplicationTests {

        @Test
        @DisplayName("Converts placeholder text in district and signalType to null without dropping row")
        void convertsPlaceholdersToNull() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1004,Uptown,unknown,Y",
                    "INT-1015,,4-way,Y",
                    "INT-1013,Westside,n/a,null"
            ));

            assertThat(result).hasSize(3);

            assertThat(result.get(0)).isEqualTo(new Intersection("INT-1004", "Uptown", null, true));
            assertThat(result.get(1)).isEqualTo(new Intersection("INT-1015", null, "4-way", true));
            assertThat(result.get(2)).isEqualTo(new Intersection("INT-1013", "Westside", null, null));
        }

        @Test
        @DisplayName("Collapses and merges duplicate records for the same intersection ID")
        void mergesDuplicateRecords() throws Exception {
            List<Intersection> result = cleaner.clean(createCsvStream(
                    "intersection_id,District,signal_type,active_flag",
                    "INT-1005,Downtown,unknown,true",
                    "int-1005,n/a,ROUNDABOUT,TRUE"
            ));

            assertThat(result).hasSize(1);
            Intersection merged = result.get(0);
            assertThat(merged.id()).isEqualTo("INT-1005");
            assertThat(merged.district()).isEqualTo("Downtown");
            assertThat(merged.signalType()).isEqualTo("roundabout");
            assertThat(merged.active()).isTrue();
        }
    }
}
