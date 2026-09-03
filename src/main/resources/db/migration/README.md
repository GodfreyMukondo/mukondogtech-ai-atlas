# Migration history note (2026-09)

`V1__init_schema.sql` and `V2__add_fraud_tables.sql` originally had a
leading space in their filenames, which meant Flyway silently ignored
both and only ever applied `V3__add_visa_rules.sql`. The filenames were
fixed to the correct `V<version>__<description>.sql` convention.

Any database that was migrated before that fix has `3` recorded in
`flyway_schema_history` with `1` and `2` missing (or, if the schema was
non-empty and Flyway baselined on first run, only `2` missing - the
baseline silently covers version 1). On the next startup Flyway's
default strict ordering will refuse to proceed:

```
FlywayValidateException: Validate failed: Migrations have failed validation
Detected resolved migration not applied to database: 2.
```

## How to recover

1. Check whether the table from the missing migration already exists
   (Oracle, case-sensitive on the literal, usually uppercase):

   ```sql
   SELECT table_name FROM user_tables WHERE table_name = 'FRAUD_ANALYTICS';
   ```

2. **If it does not exist** (the common case - this table was never
   successfully created by anything): set `FLYWAY_OUT_OF_ORDER=true` in
   the environment for one deploy/startup only
   (`spring.flyway.out-of-order` in `application.yml` reads this var).
   Flyway will apply the missing lower-numbered migration even though a
   higher one already ran. Once it succeeds, unset the variable (or
   leave it - it only matters again if this situation repeats).

3. **If it already exists** (e.g. created manually, or by an earlier
   `ddl-auto: update` run before Flyway was fully wired in): running
   the migration as-is will fail with `ORA-00955` (name already in
   use). Do not set `out-of-order` in that case. Instead, either drop
   the existing table and let Flyway create the canonical one, or
   manually insert the corresponding row into `flyway_schema_history`
   to record the migration as already applied without re-running it.
   This needs a human to confirm the existing table actually matches
   what `V2__add_fraud_tables.sql` would have created before marking it
   as done.
