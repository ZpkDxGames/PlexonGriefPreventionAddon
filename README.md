# PlexonClaimFlags — DEPRECATED

> **Deprecated:** PlexonClaimFlags / PlexonGriefPreventionAddon has been replaced by **PlexonGPFlags**.
>
> Do **not** run PlexonClaimFlags and PlexonGPFlags together. Both enforce overlapping GriefPrevention claim events and running both can create duplicate/colliding enforcement.
>
> Historical releases remain available for rollback/recovery only. The deprecated JAR is excluded from the active Plexon ecosystem deployment manifest.

## Replacement

Use [`ZpkDxGames/PlexonGPFlags`](https://github.com/ZpkDxGames/PlexonGPFlags).

PlexonGPFlags is the unified GriefPrevention claim-management and flag-control plugin. It preserves GriefPrevention authority over ownership, boundaries, trust, subdivisions and persistent claim IDs, and provides compatibility commands/API for the old ClaimFlags surface.

### Migration

On first successful startup, PlexonGPFlags can import the old `plugins/PlexonClaimFlags/flags.yml` when its own flags file does not yet exist. Keep the old plugin data directory until the migration has been verified, but remove/disable the old **JAR** before starting PlexonGPFlags.

## Historical release

PlexonClaimFlags 1.1.0 remains available as a historical rollback artifact. No further active feature or compatibility development is planned in this repository.

- Paper 26.2 / Java 25 historical target
- GriefPrevention 16.18.7+ historical requirement
- historical JAR: `PlexonClaimFlags-1.1.0.jar`

For current installation, commands, flags, APIs and migration guidance, use the PlexonGPFlags repository.
