-- ============================================================================
-- SEED: INITIAL PATHWAY CATALOGUE (PHASE 1)
-- ============================================================================
-- Seeds a SMALL, intentionally conservative set of real immigration
-- pathways so the Requirement/Pathway engine has genuine, published data to
-- evaluate against - without fabricating immigration law.
--
-- Every requirement below reflects a core, long-standing, publicly
-- documented eligibility criterion from the cited government source, and
-- is expressed only in a form the existing LogicNode grammar can represent
-- honestly:
--
--   - Criteria with a well-established, stable numeric bar that maps onto
--     an existing Fact key (e.g. "at least 1 year of work experience") are
--     encoded as a real DERIVED_PREDICATE/FACT_PREDICATE the evaluator can
--     actually check.
--   - Criteria that exist but whose current numeric threshold is volatile
--     (settlement funds amounts, minimum salary thresholds, points-based
--     cut-off scores - all of which are republished periodically by the
--     respective authority and were NOT independently re-verified as part
--     of this migration) are deliberately represented only as an
--     EXISTS-type evidence check, or omitted entirely, rather than
--     encoding a possibly-stale number as if it were durable fact. This is
--     explicitly recorded in each REGULATORY_VERSIONS.change_summary below.
--
-- Every RegulatoryVersion seeded here is recorded with
-- verification_status = 'UNVERIFIED_INGESTION' (source_type =
-- 'AUTHORITATIVE_REGULATORY_SOURCE' - it IS a primary government source,
-- but MukondoGTech AI has not yet had a human independently confirm this
-- ingestion against the live page). An administrator must review and
-- re-confirm before these should be treated as HUMAN_VERIFIED or
-- AUTHORITATIVE_CONFIRMED - see docs/evidence-intelligence-graph.md-style
-- discipline already established elsewhere in this codebase.
--
-- Both Pathways and all five Requirements are seeded directly into
-- PUBLISHED status (not DRAFT) so this migration alone is sufficient to
-- make GET /api/pathways return real, assessable data - matching the
-- Phase 1 acceptance criterion that "No pathways available yet" must
-- disappear once valid published pathways exist. Nothing prevents an
-- administrator from later superseding either pathway through the normal
-- admin lifecycle once its regulatory version has been confirmed.
-- ============================================================================

DECLARE
    v_reg_ca_id     NUMBER;
    v_reg_uk_id     NUMBER;

    v_req_ca_work   NUMBER;
    v_req_ca_edu    NUMBER;
    v_req_ca_lang   NUMBER;
    v_req_uk_job    NUMBER;
    v_req_uk_lang   NUMBER;
BEGIN

    -- =========================================================================
    -- REGULATORY VERSION: CANADA - EXPRESS ENTRY FEDERAL SKILLED WORKER PROGRAM
    -- =========================================================================
    INSERT INTO regulatory_versions (
        regulation_identity, jurisdiction, source_type, source_authority,
        source_reference, publication_date, retrieval_date, effective_from,
        effective_to, verification_status, change_summary
    ) VALUES (
        'CA_IRCC_EXPRESS_ENTRY_FSWP_ELIGIBILITY', 'Canada', 'AUTHORITATIVE_REGULATORY_SOURCE',
        'Immigration, Refugees and Citizenship Canada (IRCC)',
        'https://www.canada.ca/en/immigration-refugees-citizenship/services/immigrate-canada/express-entry/eligibility/federal-skilled-workers.html',
        NULL, CURRENT_TIMESTAMP, NULL, NULL, 'UNVERIFIED_INGESTION',
        'Initial ingestion of Federal Skilled Worker Program (Express Entry) core minimum-entry ' ||
        'criteria from IRCC public guidance. Recorded as unverified ingestion - requires ' ||
        'administrator confirmation against the live IRCC page before being marked ' ||
        'HUMAN_VERIFIED or AUTHORITATIVE_CONFIRMED. Deliberately excludes the settlement-funds ' ||
        'dollar threshold and Comprehensive Ranking System cut-off score, both of which IRCC ' ||
        'republishes periodically and were not independently re-verified as of this ingestion.'
    ) RETURNING id INTO v_reg_ca_id;

    -- =========================================================================
    -- REGULATORY VERSION: UNITED KINGDOM - SKILLED WORKER VISA
    -- =========================================================================
    INSERT INTO regulatory_versions (
        regulation_identity, jurisdiction, source_type, source_authority,
        source_reference, publication_date, retrieval_date, effective_from,
        effective_to, verification_status, change_summary
    ) VALUES (
        'UK_UKVI_SKILLED_WORKER_ELIGIBILITY', 'United Kingdom', 'AUTHORITATIVE_REGULATORY_SOURCE',
        'UK Visas and Immigration (Home Office)',
        'https://www.gov.uk/skilled-worker-visa',
        NULL, CURRENT_TIMESTAMP, NULL, NULL, 'UNVERIFIED_INGESTION',
        'Initial ingestion of Skilled Worker visa core eligibility criteria from gov.uk public ' ||
        'guidance. Recorded as unverified ingestion - requires administrator confirmation ' ||
        'against the live gov.uk page before being marked HUMAN_VERIFIED or ' ||
        'AUTHORITATIVE_CONFIRMED. Deliberately excludes the minimum salary threshold, which ' ||
        'UKVI republishes periodically and was not independently re-verified as of this ingestion.'
    ) RETURNING id INTO v_reg_uk_id;


    -- =========================================================================
    -- REQUIREMENTS: CANADA FSWP
    -- =========================================================================

    -- At least 1 year continuous skilled work experience in the last 10
    -- years - a stable, long-standing minimum-entry criterion (distinct
    -- from CRS points, which are not an eligibility bar).
    INSERT INTO requirements (
        requirement_key, requirement_type, title, description, jurisdiction,
        immigration_context, regulatory_version_id, mandatory, applicability_logic,
        satisfaction_logic, status
    ) VALUES (
        'CA_FSWP.SKILLED_WORK_EXPERIENCE', 'EXPERIENCE', 'Minimum Skilled Work Experience',
        'IRCC requires at least 1 year of continuous full-time (or equivalent part-time) paid ' ||
        'skilled work experience (TEER 0, 1, 2, or 3) within the last 10 years.',
        'Canada', 'Express Entry - Federal Skilled Worker Program', v_reg_ca_id, 1, NULL,
        '{"node":"DERIVED_PREDICATE","function":"DURATION_BETWEEN","functionArgs":["EMPLOYMENT.CURRENT_EMPLOYER"],"operator":"AT_LEAST","operandValue":"1","operandLow":null,"operandHigh":null}',
        'PUBLISHED'
    ) RETURNING id INTO v_req_ca_work;

    INSERT INTO requirement_fact_bindings (requirement_id, fact_key, requires_verification, minimum_provenance_type)
    VALUES (v_req_ca_work, 'EMPLOYMENT.CURRENT_EMPLOYER', 0, NULL);

    -- Minimum secondary education (foreign credentials require an ECA -
    -- not modelled here, existence of a recorded credential only).
    INSERT INTO requirements (
        requirement_key, requirement_type, title, description, jurisdiction,
        immigration_context, regulatory_version_id, mandatory, applicability_logic,
        satisfaction_logic, status
    ) VALUES (
        'CA_FSWP.MINIMUM_EDUCATION', 'CREDENTIAL', 'Minimum Education',
        'IRCC requires at least a Canadian secondary school (high school) credential, or a ' ||
        'foreign equivalent confirmed by an Educational Credential Assessment (ECA). ' ||
        'MukondoGTech AI currently checks only that an education credential has been recorded - ' ||
        'it does not yet confirm ECA equivalency.',
        'Canada', 'Express Entry - Federal Skilled Worker Program', v_reg_ca_id, 1, NULL,
        '{"node":"FACT_PREDICATE","factKey":"EDUCATION.DEGREE_AWARDED","operator":"EXISTS","operandValue":null,"operandValues":null,"operandLow":null,"operandHigh":null}',
        'PUBLISHED'
    ) RETURNING id INTO v_req_ca_edu;

    INSERT INTO requirement_fact_bindings (requirement_id, fact_key, requires_verification, minimum_provenance_type)
    VALUES (v_req_ca_edu, 'EDUCATION.DEGREE_AWARDED', 0, NULL);

    -- Minimum CLB 7 in all four language abilities (English or French).
    INSERT INTO requirements (
        requirement_key, requirement_type, title, description, jurisdiction,
        immigration_context, regulatory_version_id, mandatory, applicability_logic,
        satisfaction_logic, status
    ) VALUES (
        'CA_FSWP.LANGUAGE_ABILITY', 'PROFICIENCY', 'Minimum Language Ability',
        'IRCC requires a minimum of Canadian Language Benchmark (CLB) 7 in all four language ' ||
        'abilities (speaking, listening, reading, writing), in English or French. ' ||
        'MukondoGTech AI currently checks only that language test evidence has been recorded - ' ||
        'it does not yet confirm the CLB 7 threshold is met.',
        'Canada', 'Express Entry - Federal Skilled Worker Program', v_reg_ca_id, 1, NULL,
        '{"node":"FACT_PREDICATE","factKey":"LANGUAGE_PROFICIENCY.TEST_SCORE","operator":"EXISTS","operandValue":null,"operandValues":null,"operandLow":null,"operandHigh":null}',
        'PUBLISHED'
    ) RETURNING id INTO v_req_ca_lang;

    INSERT INTO requirement_fact_bindings (requirement_id, fact_key, requires_verification, minimum_provenance_type)
    VALUES (v_req_ca_lang, 'LANGUAGE_PROFICIENCY.TEST_SCORE', 0, NULL);

    INSERT INTO pathways (
        pathway_key, name, description, jurisdiction, category, composition_logic,
        evidence_expectations, valid_from, valid_to, status
    ) VALUES (
        'CA_EXPRESS_ENTRY_FSWP', 'Express Entry: Federal Skilled Worker Program',
        'A federal economic immigration pathway for skilled workers, managed through the ' ||
        'Express Entry system. This catalogue entry reflects core minimum-entry criteria only; ' ||
        'it does not model Comprehensive Ranking System (CRS) points or the settlement-funds ' ||
        'threshold, both of which change periodically - consult the official IRCC page for ' ||
        'current figures.',
        'Canada', 'Skilled Worker / Economic',
        '{"node":"AND","children":[{"node":"REQUIREMENT_REF","requirementId":' || v_req_ca_work ||
        '},{"node":"REQUIREMENT_REF","requirementId":' || v_req_ca_edu ||
        '},{"node":"REQUIREMENT_REF","requirementId":' || v_req_ca_lang || '}]}',
        'Employment records covering at least 1 year of skilled work, an education credential ' ||
        '(with ECA if obtained outside Canada), and a language test result (IELTS/CELPIP/TEF/TCF).',
        NULL, NULL, 'PUBLISHED'
    );


    -- =========================================================================
    -- REQUIREMENTS: UK SKILLED WORKER VISA
    -- =========================================================================

    -- Valid job offer from a Home Office-licensed sponsor for an eligible
    -- (RQF level 3+) occupation.
    INSERT INTO requirements (
        requirement_key, requirement_type, title, description, jurisdiction,
        immigration_context, regulatory_version_id, mandatory, applicability_logic,
        satisfaction_logic, status
    ) VALUES (
        'UK_SKILLED_WORKER.SPONSORED_JOB_OFFER', 'PROCEDURAL', 'Sponsored Job Offer',
        'UKVI requires a valid job offer from a Home Office-licensed sponsor, for an eligible ' ||
        'skilled occupation (RQF level 3 or above). MukondoGTech AI currently checks only that ' ||
        'an employer/job record exists - it does not yet confirm sponsor-licence status or the ' ||
        'occupation''s skill level.',
        'United Kingdom', 'Skilled Worker visa', v_reg_uk_id, 1, NULL,
        '{"node":"FACT_PREDICATE","factKey":"EMPLOYMENT.CURRENT_EMPLOYER","operator":"EXISTS","operandValue":null,"operandValues":null,"operandLow":null,"operandHigh":null}',
        'PUBLISHED'
    ) RETURNING id INTO v_req_uk_job;

    INSERT INTO requirement_fact_bindings (requirement_id, fact_key, requires_verification, minimum_provenance_type)
    VALUES (v_req_uk_job, 'EMPLOYMENT.CURRENT_EMPLOYER', 0, NULL);

    -- Minimum CEFR B1 English (equivalent to IELTS 4.0), a stable,
    -- long-standing Skilled Worker route requirement.
    INSERT INTO requirements (
        requirement_key, requirement_type, title, description, jurisdiction,
        immigration_context, regulatory_version_id, mandatory, applicability_logic,
        satisfaction_logic, status
    ) VALUES (
        'UK_SKILLED_WORKER.ENGLISH_LANGUAGE', 'PROFICIENCY', 'Minimum English Language Ability',
        'UKVI requires English language ability at a minimum of CEFR level B1 (equivalent to ' ||
        'IELTS 4.0 in each component), unless exempt. MukondoGTech AI currently checks only ' ||
        'that language test evidence has been recorded - it does not yet confirm the B1 ' ||
        'threshold is met.',
        'United Kingdom', 'Skilled Worker visa', v_reg_uk_id, 1, NULL,
        '{"node":"FACT_PREDICATE","factKey":"LANGUAGE_PROFICIENCY.TEST_SCORE","operator":"EXISTS","operandValue":null,"operandValues":null,"operandLow":null,"operandHigh":null}',
        'PUBLISHED'
    ) RETURNING id INTO v_req_uk_lang;

    INSERT INTO requirement_fact_bindings (requirement_id, fact_key, requires_verification, minimum_provenance_type)
    VALUES (v_req_uk_lang, 'LANGUAGE_PROFICIENCY.TEST_SCORE', 0, NULL);

    INSERT INTO pathways (
        pathway_key, name, description, jurisdiction, category, composition_logic,
        evidence_expectations, valid_from, valid_to, status
    ) VALUES (
        'UK_SKILLED_WORKER', 'Skilled Worker Visa',
        'A UK sponsored-work immigration route for applicants with a qualifying job offer from ' ||
        'a licensed sponsor. This catalogue entry reflects core eligibility criteria only; it ' ||
        'does not model the minimum salary threshold, which UKVI republishes periodically - ' ||
        'consult the official gov.uk page for current figures.',
        'United Kingdom', 'Skilled Worker / Sponsored Employment',
        '{"node":"AND","children":[{"node":"REQUIREMENT_REF","requirementId":' || v_req_uk_job ||
        '},{"node":"REQUIREMENT_REF","requirementId":' || v_req_uk_lang || '}]}',
        'A certificate of sponsorship reference/employer record and an English language test ' ||
        'result (or evidence of an exempt nationality/qualification).',
        NULL, NULL, 'PUBLISHED'
    );

END;
/
