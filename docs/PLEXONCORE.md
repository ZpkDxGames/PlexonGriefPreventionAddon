# PlexonCore Integration

PlexonClaimFlags 1.1.0 supports PlexonCore API `>=1.0 <2.0`.

## Ownership boundaries

PlexonCore owns module registration, ecosystem health/diagnostics, and shared infrastructure. GriefPrevention owns claims, ownership, trust, boundaries, subdivisions, administrative claims, and persistent claim IDs. PlexonClaimFlags owns only explicit flag overrides, effective flag resolution, protection listeners, GUI/commands, `flags.yml`, and the public ClaimFlags API/event.

PlexonCore is compile-only/provided. `CoreBridgeFactory` resolves the Core-specific bridge reflectively so the optional Core plugin can be absent without a class-linkage failure. CI verifies no `com/zpkdxgames/plexoncore/` runtime classes are shaded into the plugin JAR.

With compatible Core, module ID `claimflags` registers STARTING then READY and publishes ClaimFlags integration capabilities. With Core absent, disabled, or incompatible, the protection engine remains STANDALONE. GriefPrevention is still a required plugin dependency.

Protection listeners do not call PlexonCore for block, interaction, spawn, PvP, explosion, fire, crop, or mob-griefing decisions.

Use:

```text
/plexon modules
/plexon integrations
/plexon diagnostics
/claimsflags diagnostics
```
