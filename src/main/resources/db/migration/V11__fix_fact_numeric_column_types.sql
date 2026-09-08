-- ============================================================================
-- FIX FACT NUMERIC COLUMN TYPES
-- ============================================================================
-- V10 declared FACTS.CONFIDENCE_SCORE/NUMBER_VALUE as plain NUMBER. Hibernate
-- maps the entity's java.lang.Double fields to FLOAT(53) by default on
-- Oracle, which schema-validation (ddl-auto: validate) rejects against a
-- plain NUMBER column - this failed application startup entirely.
--
-- Fixed by widening both columns to an explicit NUMBER(precision,scale) and
-- pairing that with a matching columnDefinition on the entity (the same
-- pattern already used for Application.aiConfidence / APPLICATIONS.AI_
-- CONFIDENCE NUMBER(5,2)), rather than switching V10 itself - V10 is already
-- applied and its checksum recorded, so it is never edited in place.
--
-- No data loss: FACTS has no rows referencing these columns yet at the time
-- this migration is introduced.
-- ============================================================================

ALTER TABLE facts MODIFY (confidence_score NUMBER(5,4));
ALTER TABLE facts MODIFY (number_value NUMBER(19,4));
