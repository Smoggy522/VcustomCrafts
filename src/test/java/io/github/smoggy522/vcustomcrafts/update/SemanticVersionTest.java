package io.github.smoggy522.vcustomcrafts.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test
    void comparesReleaseVersions() {
        assertTrue(SemanticVersion.parse("v1.2.0").compareTo(SemanticVersion.parse("1.1.9")) > 0);
        assertTrue(SemanticVersion.parse("1.0.0").compareTo(SemanticVersion.parse("1.0.0-beta.1")) > 0);
        assertEquals(0, SemanticVersion.parse("1.0").compareTo(SemanticVersion.parse("1.0.0")));
    }
}

