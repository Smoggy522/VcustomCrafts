package io.github.smoggy522.vcustomcrafts.update;

import io.github.smoggy522.vcustomcrafts.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public final class GitHubUpdateService {
    private final JavaPlugin plugin;
    private final Messages messages;
    private final HttpClient client;

    public GitHubUpdateService(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("updates.enabled", true)) {
            return;
        }
        long hours = Math.max(1, plugin.getConfig().getLong("updates.check-interval-hours", 6));
        long period = hours * 60L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
            () -> check(Bukkit.getConsoleSender(), automaticDownload()), 100L, period);
    }

    public void check(CommandSender receiver, boolean download) {
        CompletableFuture.runAsync(() -> {
            try {
                GitHubRelease release = fetchLatest();
                SemanticVersion current = SemanticVersion.parse(plugin.getPluginMeta().getVersion());
                SemanticVersion latest = SemanticVersion.parse(release.tag());
                if (latest.compareTo(current) <= 0) {
                    sync(() -> messages.send(receiver, "update-current",
                        Map.of("version", plugin.getPluginMeta().getVersion())));
                    return;
                }
                if (!download) {
                    sync(() -> messages.send(receiver, "update-available",
                        Map.of("version", release.tag(), "url", release.pageUri())));
                    return;
                }
                stage(release);
                sync(() -> messages.send(receiver, "update-staged", Map.of("version", release.tag())));
            } catch (Exception exception) {
                plugin.getLogger().warning("Update check failed: " + exception.getMessage());
                sync(() -> messages.send(receiver, "update-error"));
            }
        });
    }

    private GitHubRelease fetchLatest() throws IOException, InterruptedException {
        String owner = plugin.getConfig().getString("updates.owner", "Smoggy522");
        String repository = plugin.getConfig().getString("updates.repository", "VcustomCrafts");
        if (!owner.matches("[A-Za-z0-9_.-]+") || !repository.matches("[A-Za-z0-9_.-]+")) {
            throw new IOException("Invalid GitHub repository configuration");
        }
        URI uri = URI.create("https://api.github.com/repos/" + owner + "/" + repository + "/releases/latest");
        HttpRequest.Builder request = baseRequest(uri).header("Accept", "application/vnd.github+json");
        addToken(request);
        HttpResponse<String> response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("GitHub returned HTTP " + response.statusCode());
        }
        String configuredPattern = plugin.getConfig().getString("updates.asset-pattern", "^VcustomCrafts-.+\\.jar$");
        return GitHubRelease.parse(response.body(), Pattern.compile(configuredPattern));
    }

    private void stage(GitHubRelease release) throws IOException, InterruptedException, NoSuchAlgorithmException {
        boolean requireDigest = plugin.getConfig().getBoolean("updates.require-sha256", true);
        if (requireDigest && (release.sha256() == null || !release.sha256().matches("[0-9a-f]{64}"))) {
            throw new IOException("Release asset has no valid SHA-256 digest");
        }
        String token = token();
        URI source = token == null ? release.downloadUri() : release.assetApiUri();
        requireGitHubUri(source);
        HttpRequest.Builder request = baseRequest(source).header("Accept", "application/octet-stream");
        addToken(request);
        HttpResponse<InputStream> response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            response.body().close();
            throw new IOException("Asset download returned HTTP " + response.statusCode());
        }

        long limit = Math.max(1, plugin.getConfig().getLong("updates.max-download-megabytes", 50)) * 1024L * 1024L;
        long declared = response.headers().firstValueAsLong("content-length").orElse(-1L);
        if (declared > limit) {
            response.body().close();
            throw new IOException("Release asset exceeds configured size limit");
        }

        Path updateDirectory = plugin.getServer().getUpdateFolderFile().toPath();
        Files.createDirectories(updateDirectory);
        Path target = updateDirectory.resolve(currentJarName());
        Path temporary = Files.createTempFile(updateDirectory, "vcustomcrafts-", ".jar.tmp");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long bytes = 0;
        try (InputStream raw = response.body();
             DigestInputStream input = new DigestInputStream(new BufferedInputStream(raw), digest);
             var output = Files.newOutputStream(temporary)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes += read;
                if (bytes > limit) {
                    throw new IOException("Release asset exceeds configured size limit");
                }
                output.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }

        String actual = HexFormat.of().formatHex(digest.digest());
        if (release.sha256() != null && !MessageDigest.isEqual(
            actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            release.sha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            Files.deleteIfExists(temporary);
            throw new IOException("Release asset SHA-256 verification failed");
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60))
            .header("User-Agent", "VcustomCrafts/" + plugin.getPluginMeta().getVersion())
            .header("X-GitHub-Api-Version", "2026-03-10");
    }

    private void addToken(HttpRequest.Builder request) {
        String token = token();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
    }

    private String token() {
        String variable = plugin.getConfig().getString("updates.token-environment-variable",
            "VCUSTOMCRAFTS_GITHUB_TOKEN");
        String value = variable == null ? null : System.getenv(variable);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean automaticDownload() {
        return "DOWNLOAD".equalsIgnoreCase(plugin.getConfig().getString("updates.mode", "NOTIFY"));
    }

    private String currentJarName() {
        try {
            Path location = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            String name = location.getFileName().toString();
            return name.toLowerCase(Locale.ROOT).endsWith(".jar") ? name : "VcustomCrafts.jar";
        } catch (Exception exception) {
            return "VcustomCrafts.jar";
        }
    }

    private static void requireGitHubUri(URI uri) throws IOException {
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
            || !(host.equalsIgnoreCase("api.github.com") || host.equalsIgnoreCase("github.com"))) {
            throw new IOException("Refusing non-GitHub update URL");
        }
    }

    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
