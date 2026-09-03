# PlexonClaimFlags

A lightweight GriefPrevention addon for **Paper 26.2** that gives claim owners a GUI and commands for extra per-claim protection flags.

## Requirements

- Paper 26.2
- Java 25
- GriefPrevention 16.18.7+

GriefPrevention remains the authority for claim ownership, boundaries, trust and subdivisions. PlexonClaimFlags only layers optional restrictions on top.

## First release: included flags

| Flag | Effect when ON |
|---|---|
| `natural-mobs` | Cancels configured natural creature spawn reasons inside the claim. |
| `spawner-mobs` | Cancels mob spawns produced by mob-spawner blocks. |
| `pvp` | Prevents player-vs-player damage when either participant is inside a protected claim/subclaim. |
| `building` | Prevents non-owner players from placing or breaking blocks even if GriefPrevention would otherwise trust them. |
| `interactions` | Prevents non-owner use of configured doors, buttons, pressure plates, workstations, furnaces, etc. |
| `containers` | Separately prevents non-owner access to configured chests/containers. |
| `explosions` | Removes protected claim blocks from explosion block-damage lists. |
| `fire` | Prevents ignition, spread and block burning inside the claim. |
| `crop-trampling` | Prevents farmland trampling. |
| `mob-griefing` | Prevents common entity block changes and mob-caused explosion damage inside the claim. |

A player with `plexonclaimflags.bypass` bypasses player-targeted restrictions. Claim owners automatically bypass **building / interaction / container** restrictions on their own claim, but not world-behavior flags such as PvP, spawning, fire or explosions.

## GUI

Stand inside a claim or subdivision and run:

```text
/claimsflags
```

- **Left-click** a flag to toggle an explicit value.
- **Right-click** a subclaim flag to remove its override and inherit from the parent claim.
- Right-clicking a main-claim flag resets it to the configured server default.
- **Claim Areas** opens the parent claim plus all detected subdivisions, with pagination.

If the player is already standing inside a subdivision, `/claimsflags` opens that exact subdivision.

## Commands

```text
/claimsflags
/claimsflags areas
/claimsflags list [current|parent|sub:<number>]
/claimsflags set <flag> <on|off|inherit> [current|parent|sub:<number>]
/claimsflags parent
/claimsflags sub <number>
/claimsflags reload
```

Aliases: `/claimflags`, `/cf`

`inherit` is valid for subdivisions when `settings.subclaims-inherit-parent: true`.

Examples:

```text
/claimsflags set pvp on parent
/claimsflags set spawner-mobs off sub:2
/claimsflags list sub:1
```

## Permissions

| Permission | Default | Purpose |
|---|---:|---|
| `plexonclaimflags.use` | true | Use `/claimsflags` on claims the player owns. |
| `plexonclaimflags.admin` | op | Configure any player claim and reload the addon. |
| `plexonclaimflags.adminclaims` | op | Configure GriefPrevention administrative claims. |
| `plexonclaimflags.bypass` | op | Bypass player-targeted addon restrictions. |

## Persistence and performance

Runtime flag checks are served from an in-memory map. `flags.yml` is only touched when a flag changes, a claim is deleted, the plugin reloads, or the server shuts down. Block-break/place, interaction and mob-spawn events do not read from disk.

Stored values are keyed by GriefPrevention's persistent claim ID. Subclaims have their own IDs and can hold their own overrides.

## Configuration

`config.yml` controls defaults, inheritance, spawn reasons, interaction/container materials, GUI presentation, messages and flag display names.

`flags.yml` is generated automatically and contains only explicit claim/subclaim overrides.

## Build

The project targets Java 25 and Paper 26.2.

```bash
gradle clean build
```

Output: `build/libs/PlexonClaimFlags-1.0.0.jar`.
