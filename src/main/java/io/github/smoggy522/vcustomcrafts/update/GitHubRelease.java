package io.github.smoggy522.vcustomcrafts.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.regex.Pattern;

public record GitHubRelease(String tag, URI pageUri, URI assetApiUri, URI downloadUri,
                            String assetName, String sha256) {
    public static GitHubRelease parse(String json, Pattern assetPattern) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String tag = requiredString(root, "tag_name");
        URI page = URI.create(requiredString(root, "html_url"));
        JsonArray assets = root.getAsJsonArray("assets");
        if (assets == null) {
            throw new IllegalArgumentException("Release contains no assets array");
        }
        for (JsonElement element : assets) {
            JsonObject asset = element.getAsJsonObject();
            String name = requiredString(asset, "name");
            if (!assetPattern.matcher(name).matches()) {
                continue;
            }
            URI api = URI.create(requiredString(asset, "url"));
            URI download = URI.create(requiredString(asset, "browser_download_url"));
            String digest = optionalString(asset, "digest");
            String sha256 = digest != null && digest.startsWith("sha256:")
                ? digest.substring("sha256:".length()).toLowerCase() : null;
            return new GitHubRelease(tag, page, api, download, name, sha256);
        }
        throw new IllegalArgumentException("Release has no matching JAR asset");
    }

    private static String requiredString(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitHub response is missing '" + key + "'");
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}

