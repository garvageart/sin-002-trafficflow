package co.wethinkcode.trafficflow;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class IntersectionCsvCleaner {
    private static final Set<String> PLACEHOLDERS = Set.of("", "n/a", "na", "tbd", "unknown", "-", "nan", "null");

    public List<Intersection> cleanFromClasspath(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return clean(in);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to read CSV: " + resourcePath, e);
        }
    }

    public List<Intersection> clean(InputStream inputStream) throws IOException, CsvException {
        Map<String, Intersection> cleanedMap = new LinkedHashMap<>();

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).build()) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                return List.of();
            }

            boolean isHeader = true;
            for (String[] row : rows) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                if (row.length == 0) {
                    continue;
                }

                String rawId = row[0];
                String rawDistrict = row.length > 1 ? row[1] : null;
                String rawSignalType = row.length > 2 ? row[2] : null;
                String rawActive = row.length > 3 ? row[3] : null;

                String id = cleanText(rawId);
                if (id == null) {
                    continue;
                }

                id = id.toUpperCase(Locale.ROOT);

                String district = cleanText(rawDistrict);
                if (district != null) {
                    district = Character.toUpperCase(district.charAt(0)) + district.substring(1).toLowerCase(Locale.ROOT);
                }

                String signalType = cleanText(rawSignalType);
                if (signalType != null) {
                    signalType = signalType.toLowerCase(Locale.ROOT);
                }

                Boolean active = cleanActiveFlag(rawActive);
                Intersection newRecord = new Intersection(id, district, signalType, active);

                if (cleanedMap.containsKey(id)) {
                    Intersection existing = cleanedMap.get(id);

                    String mergedDistrict = existing.district() != null ? existing.district() : district;
                    String mergedSignal = existing.signalType() != null ? existing.signalType() : signalType;

                    Boolean mergedActive = existing.active() != null ? existing.active() : active;
                    cleanedMap.put(id, new Intersection(id, mergedDistrict, mergedSignal, mergedActive));
                } else {
                    cleanedMap.put(id, newRecord);
                }
            }
        }

        return new ArrayList<>(cleanedMap.values());
    }

    private String cleanText(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty() || PLACEHOLDERS.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return null;
        }

        return trimmed;
    }

    private Boolean cleanActiveFlag(String raw) {
        String value = cleanText(raw);
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (Set.of("y", "yes", "true", "1").contains(lower)) {
            return Boolean.TRUE;
        }
        if (Set.of("n", "no", "false", "0").contains(lower)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
