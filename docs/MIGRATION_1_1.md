# Migration: PlexonClaimFlags 1.0.0 → 1.1.0

1. Stop the server.
2. Back up `PlexonClaimFlags-1.0.0.jar`.
3. Back up `plugins/PlexonClaimFlags/`.
4. Replace the old JAR with `PlexonClaimFlags-1.1.0.jar`.
5. Keep the existing `config.yml`.
6. Keep the existing `flags.yml`.
7. Start the server.
8. Run `/plexon modules` and `/claimsflags diagnostics` when PlexonCore is installed.
9. Test one controlled claim/subclaim flag and restore its original value.

Expected with PlexonCore 1.0.0: module `PlexonClaimFlags` is READY and mode is CORE. Without PlexonCore, the plugin should start in STANDALONE mode and preserve all ClaimFlags functionality.

Rollback remains possible by stopping the server and restoring the 1.0.0 JAR plus the plugin-folder backup only if necessary. 1.1.0 intentionally does not introduce a data migration that prevents rollback.
