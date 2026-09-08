# Changelog

## 1.1.0

- Adopted PlexonCore module integration with `claimflags`, Core API range `>=1.0 <2.0`, lifecycle health, integration capabilities, and standalone fallback.
- Added public Bukkit `PlexonClaimFlagsAPI` and synchronous post-success `PlexonClaimFlagChangedEvent` with transaction/event IDs.
- Added `/claimsflags diagnostics`.
- Centralized GUI/command/API mutations through one authorized durable mutation service to prevent duplicate events.
- Preserved all ten existing flag IDs, protection semantics, owner/bypass behavior, subclaim inheritance, commands, permissions, and `flags.yml` format.
- Kept runtime protection checks memory-backed with no PlexonCore or file lookup in gameplay hot paths.
- Added safer `flags.yml` writes with temporary-file replacement and mutation rollback on persistence failure.
- Normalized the repository to a standard Java/Gradle source tree.
- Replaced source-reconstruction CI and the hard-coded 1.0.0 publisher with direct-source verification and a tag-driven release workflow.
