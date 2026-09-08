# PlexonClaimFlags

**PlexonClaimFlags 1.1.0** is a lightweight GriefPrevention addon for **Paper 26.2 / Java 25**. GriefPrevention remains authoritative for claim ownership, boundaries, trust, subdivisions, administrative claims, and persistent claim IDs. PlexonClaimFlags owns only its additional flag state and enforcement.

## Requirements and runtime modes

- Paper 26.2
- Java 25
- GriefPrevention 16.18.7+ — required
- PlexonCore 1.0.0 / Core API 1.x — optional

With a compatible PlexonCore, the plugin registers module `claimflags` and reports **CORE / READY**. Without PlexonCore it runs in **STANDALONE** mode with the same protection, GUI, commands, API, and events. PlexonCore is compile-only and is never shaded into this JAR.

## Existing flags preserved in 1.1.0

| Flag | Effect when ON |
|---|---|
| `natural-mobs` | Cancels configured natural creature spawn reasons inside the claim. |
| `spawner-mobs` | Cancels mob spawns produced by mob-spawner blocks. |
| `pvp` | Prevents player-vs-player damage when either participant is inside a protected claim/subclaim. |
| `building` | Prevents non-owner block placement/breaking even when GriefPrevention trust would otherwise allow it. |
| `interactions` | Prevents non-owner use of configured interactive blocks. |
| `containers` | Separately prevents non-owner access to configured containers. |
| `explosions` | Removes protected claim blocks from explosion block-damage lists. |
| `fire` | Prevents ignition, spread, and block burning. |
| `crop-trampling` | Prevents farmland trampling. |
| `mob-griefing` | Prevents the existing supported entity block-change/mob explosion paths. |

`plexonclaimflags.bypass` remains a player-targeted bypass. Claim owners automatically bypass building/interactions/containers on their own claims, but do not automatically bypass world-behavior flags such as PvP, spawning, fire, or explosions.

## GUI and inheritance

Stand in a claim/subdivision and run `/claimsflags`. Left-click creates/toggles an explicit value. Right-click on a main claim resets that flag to the configured server default. Right-click on a subdivision removes its explicit override and restores parent inheritance when `settings.subclaims-inherit-parent: true`.

`/claimsflags` continues to open the exact subdivision the player is standing in. The Claim Areas browser preserves parent/subclaim navigation and pagination.

## Commands

```text
/claimsflags
/claimsflags areas
/claimsflags list [current|parent|sub:<number>]
/claimsflags set <flag> <on|off|inherit> [current|parent|sub:<number>]
/claimsflags parent
/claimsflags sub <number>
/claimsflags reload
/claimsflags diagnostics
```

Aliases: `/claimflags`, `/cf`.

## Permissions

| Permission | Default | Purpose |
|---|---:|---|
| `plexonclaimflags.use` | true | Manage flags on owned claims. |
| `plexonclaimflags.admin` | op | Manage player claims and administrative plugin commands. |
| `plexonclaimflags.adminclaims` | op | Manage GriefPrevention administrative claims. |
| `plexonclaimflags.bypass` | op | Bypass player-targeted ClaimFlags restrictions. |

## Persistence and hot-path performance

Explicit overrides remain in `plugins/PlexonClaimFlags/flags.yml`, keyed by GriefPrevention persistent claim ID. Runtime protection decisions use the in-memory flag map; gameplay listeners do not read `flags.yml` or resolve PlexonCore. 1.1.0 adds safe temporary-file replacement for infrequent flag writes and rolls back an in-memory mutation if durable persistence fails.

## Public API and event

1.1.0 registers `net.plexon.claimflags.api.PlexonClaimFlagsAPI` through Bukkit `ServicesManager` in both CORE and STANDALONE modes. Successful explicit mutations emit one synchronous `net.plexon.claimflags.event.PlexonClaimFlagChangedEvent` with old/new explicit and effective values plus transaction/event IDs. See [`docs/API.md`](docs/API.md).

## Upgrade from 1.0.0

Replace the JAR only. Keep the existing `config.yml` and `flags.yml`; no data reset or SQLite migration is required. See [`docs/MIGRATION_1_1.md`](docs/MIGRATION_1_1.md).

## Build

The source tree is now maintained normally in Git. PlexonCore 1.0.0 must be available in the local Maven repository for compilation.

```bash
gradle clean check
```

Output: `build/libs/PlexonClaimFlags-1.1.0.jar`.
