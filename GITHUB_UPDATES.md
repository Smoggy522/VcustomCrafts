# GitHub Releases and automatic updates

The updater is already configured for:

```text
https://github.com/Smoggy522/VcustomCrafts
```

It reads the latest published, non-draft, non-prerelease GitHub Release. A plain pushed commit or Git tag is not an update; the tag workflow must create a Release with a matching JAR asset.

## Publish a release

1. Change `version=` in `gradle.properties`, for example to `1.1.0`.
2. Commit and push the change.
3. Create and push a matching tag:

```bash
git tag v1.1.0
git push origin main --tags
```

The included `release.yml` workflow builds Java 21 sources and attaches `VcustomCrafts-1.1.0.jar` to the release. The workflow rejects a tag that does not match `gradle.properties`.

## Update modes

```yaml
updates:
  enabled: true
  mode: DOWNLOAD # or NOTIFY
```

- `NOTIFY` reports the new version.
- `DOWNLOAD` verifies the asset and puts it in Paper's update directory. Paper installs it on the next full restart.

The updater never restarts the server and never executes remote commands.

## Private repository

Do not put a token in `config.yml`. Define the environment variable named by `token-environment-variable`:

```text
VCUSTOMCRAFTS_GITHUB_TOKEN
```

Use a fine-grained GitHub token that can read this repository's Contents. Start the Minecraft process with that environment variable available. Public repositories do not need a token.

## Security checks

- only the configured GitHub repository's latest published Release is queried;
- the asset filename must match `asset-pattern`;
- downloads have a size limit;
- the GitHub-provided SHA-256 digest is required by default;
- the downloaded file is staged atomically;
- installation happens only on the next server restart.

