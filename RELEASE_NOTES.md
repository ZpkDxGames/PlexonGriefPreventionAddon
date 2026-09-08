# PlexonClaimFlags 1.1.0

PlexonClaimFlags 1.1.0 is a conservative PlexonCore/source/API migration. Existing GriefPrevention claim behavior and ClaimFlags data remain authoritative and compatible.

### Added
- PlexonCore module `claimflags` with CORE/STANDALONE modes and lifecycle diagnostics.
- Bukkit `PlexonClaimFlagsAPI` service.
- `PlexonClaimFlagChangedEvent` with change source, old/new state, transaction ID, and event ID.
- `/claimsflags diagnostics`.
- Direct-source CI, distribution checks, no-Core-shading verification, and SHA-256 release checksums.

### Preserved
- All ten 1.0.0 flag IDs and enforcement semantics.
- GriefPrevention authority over claims, trust, ownership, boundaries, and subdivisions.
- Existing `config.yml` and `flags.yml` compatibility.
- Main-claim reset and subclaim inheritance behavior.
- In-memory protection hot paths.

Upgrade by stopping the server, backing up the old JAR/plugin folder, replacing only the JAR, and keeping `config.yml` and `flags.yml`.
