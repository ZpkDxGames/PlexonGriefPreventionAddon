# PlexonClaimFlags 1.1.0 API

## ServicesManager lookup

```java
RegisteredServiceProvider<PlexonClaimFlagsAPI> registration =
    Bukkit.getServicesManager().getRegistration(PlexonClaimFlagsAPI.class);
PlexonClaimFlagsAPI api = registration.getProvider();
```

The service is registered in both CORE and STANDALONE modes and is removed on plugin disable.

## Explicit vs effective values

`explicitValue(claimId, flagId)` returns the stored override, if present. `effectiveValue(...)` resolves an explicit value first, then parent inheritance for subdivisions when enabled, then the configured server default. `FlagOverride.INHERIT` removes the explicit value; on a subdivision this restores inheritance, while internal GUI reset semantics on a main claim restore the server default.

## Mutation authorization

`setExplicit(Player actor, long claimId, String flagId, FlagOverride value)` uses the same ownership, `plexonclaimflags.admin`, and `plexonclaimflags.adminclaims` rules as the GUI/commands. It does not expose a privileged bypass path.

Mutations must run on the primary server thread. API reads that resolve GriefPrevention claims must also run on the primary thread; the API does not advertise asynchronous GriefPrevention claim resolution.

## Event

`PlexonClaimFlagChangedEvent` is synchronous and fires once only after a real, durably persisted explicit state change. It does not fire for GUI rendering, list operations, startup/config loading, failed authorization, invalid targets, or no-op mutations.

Fields include actor, claim ID, optional parent claim ID, subdivision indicator, flag ID, old/new explicit state, old/new effective state, change source, transaction ID, and event ID. A mutation uses a UUID transaction ID and an event ID shaped like `<transactionId>:flag:<flagId>`.
