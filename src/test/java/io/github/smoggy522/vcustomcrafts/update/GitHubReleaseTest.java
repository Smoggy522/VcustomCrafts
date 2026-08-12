package io.github.smoggy522.vcustomcrafts.update;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHubReleaseTest {
    @Test
    void selectsMatchingJarAndDigest() {
        String json = """
            {"tag_name":"v1.2.3","html_url":"https://github.com/Smoggy522/VcustomCrafts/releases/tag/v1.2.3",
             "assets":[{"name":"sources.zip","url":"https://api.github.com/a","browser_download_url":"https://github.com/a"},
             {"name":"VcustomCrafts-1.2.3.jar","url":"https://api.github.com/jar","browser_download_url":"https://github.com/jar",
             "digest":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]}
            """;
        GitHubRelease release = GitHubRelease.parse(json,
            Pattern.compile("^VcustomCrafts-[0-9.]+\\.jar$"));
        assertEquals("v1.2.3", release.tag());
        assertEquals("a".repeat(64), release.sha256());
    }
}

