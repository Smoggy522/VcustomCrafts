package io.github.smoggy522.vcustomcrafts.update;

import java.util.ArrayList;
import java.util.List;

public record SemanticVersion(List<Integer> numbers, String qualifier) implements Comparable<SemanticVersion> {
    public SemanticVersion {
        numbers = List.copyOf(numbers);
        qualifier = qualifier == null ? "" : qualifier;
    }

    public static SemanticVersion parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Version is empty");
        }
        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        String[] buildSplit = normalized.split("\\+", 2);
        String[] qualifierSplit = buildSplit[0].split("-", 2);
        String[] parts = qualifierSplit[0].split("\\.");
        List<Integer> numbers = new ArrayList<>();
        for (String part : parts) {
            if (!part.matches("[0-9]+")) {
                throw new IllegalArgumentException("Invalid semantic version: " + value);
            }
            numbers.add(Integer.parseInt(part));
        }
        String qualifier = qualifierSplit.length == 2 ? qualifierSplit[1] : "";
        return new SemanticVersion(numbers, qualifier);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int length = Math.max(numbers.size(), other.numbers.size());
        for (int index = 0; index < length; index++) {
            int left = index < numbers.size() ? numbers.get(index) : 0;
            int right = index < other.numbers.size() ? other.numbers.get(index) : 0;
            int comparison = Integer.compare(left, right);
            if (comparison != 0) {
                return comparison;
            }
        }
        if (qualifier.isEmpty() && !other.qualifier.isEmpty()) {
            return 1;
        }
        if (!qualifier.isEmpty() && other.qualifier.isEmpty()) {
            return -1;
        }
        return qualifier.compareToIgnoreCase(other.qualifier);
    }
}

