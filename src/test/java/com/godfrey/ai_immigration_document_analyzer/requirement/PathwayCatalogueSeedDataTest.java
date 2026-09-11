package com.godfrey.ai_immigration_document_analyzer.requirement;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * PATHWAY CATALOGUE SEED DATA TEST
 * ============================================================================
 *
 * Verifies migration {@code V14__seed_pathway_catalogue.sql} actually
 * applied and produced the exact, real data described in its own header -
 * against the real, configured Oracle instance (matching the one existing
 * precedent for a full-context test in this codebase,
 * {@code AiImmigrationDocumentAnalyzerApplicationTests}), not a mocked
 * repository. This is "seed/migration verification," not a unit test of
 * any service's logic.
 * ============================================================================
 */
@SpringBootTest
class PathwayCatalogueSeedDataTest {

    @Autowired
    private PathwayRepository pathwayRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private RequirementFactBindingRepository factBindingRepository;

    @Autowired
    private RegulatoryVersionRepository regulatoryVersionRepository;

    @Test
    void twoPathwaysAreSeededAndPublished() {

        Optional<Pathway> ca = pathwayRepository.findByPathwayKey("CA_EXPRESS_ENTRY_FSWP");
        Optional<Pathway> uk = pathwayRepository.findByPathwayKey("UK_SKILLED_WORKER");

        assertThat(ca).isPresent();
        assertThat(ca.get().getStatus()).isEqualTo(PathwayStatus.PUBLISHED);
        assertThat(ca.get().getJurisdiction()).isEqualTo("Canada");

        assertThat(uk).isPresent();
        assertThat(uk.get().getStatus()).isEqualTo(PathwayStatus.PUBLISHED);
        assertThat(uk.get().getJurisdiction()).isEqualTo("United Kingdom");
    }

    @Test
    void publishedPathwaysAreReturnedByTheExactQueryThePublicApiUses() {

        // Mirrors PathwayController.listPublishedPathways() exactly.
        List<Pathway> published = pathwayRepository.findByStatus(PathwayStatus.PUBLISHED);

        assertThat(published)
                .extracting(Pathway::getPathwayKey)
                .contains("CA_EXPRESS_ENTRY_FSWP", "UK_SKILLED_WORKER");
    }

    @Test
    void fiveRequirementsAreSeededPublishedAndBoundToRealFactKeys() {

        List<String> expectedKeys = List.of(
                "CA_FSWP.SKILLED_WORK_EXPERIENCE",
                "CA_FSWP.MINIMUM_EDUCATION",
                "CA_FSWP.LANGUAGE_ABILITY",
                "UK_SKILLED_WORKER.SPONSORED_JOB_OFFER",
                "UK_SKILLED_WORKER.ENGLISH_LANGUAGE"
        );

        for (String key : expectedKeys) {

            List<Requirement> matches = requirementRepository.findByRequirementKey(key);

            assertThat(matches).as("requirement " + key).hasSize(1);

            Requirement requirement = matches.get(0);
            assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.PUBLISHED);
            assertThat(requirement.getRegulatoryVersionId()).isNotNull();
            assertThat(requirement.getSatisfactionLogic()).contains("\"node\"");

            List<RequirementFactBinding> bindings = factBindingRepository.findByRequirementId(requirement.getId());
            assertThat(bindings).as("bindings for " + key).isNotEmpty();
        }
    }

    @Test
    void bothRegulatoryVersionsAreRecordedAsUnverifiedIngestionWithARealSourceReference() {

        List<RegulatoryVersion> ca = regulatoryVersionRepository.findByRegulationIdentity(
                "CA_IRCC_EXPRESS_ENTRY_FSWP_ELIGIBILITY"
        );
        List<RegulatoryVersion> uk = regulatoryVersionRepository.findByRegulationIdentity(
                "UK_UKVI_SKILLED_WORKER_ELIGIBILITY"
        );

        assertThat(ca).hasSize(1);
        assertThat(ca.get(0).getSourceReference()).startsWith("https://www.canada.ca/");
        assertThat(ca.get(0).getVerificationStatus().name()).isEqualTo("UNVERIFIED_INGESTION");

        assertThat(uk).hasSize(1);
        assertThat(uk.get(0).getSourceReference()).startsWith("https://www.gov.uk/");
        assertThat(uk.get(0).getVerificationStatus().name()).isEqualTo("UNVERIFIED_INGESTION");
    }

    @Test
    void pathwayCompositionLogicReferencesItsOwnSeededRequirementsOnly() {

        Pathway ca = pathwayRepository.findByPathwayKey("CA_EXPRESS_ENTRY_FSWP").orElseThrow();

        Requirement workExperience = requirementRepository.findByRequirementKey("CA_FSWP.SKILLED_WORK_EXPERIENCE").get(0);
        Requirement education = requirementRepository.findByRequirementKey("CA_FSWP.MINIMUM_EDUCATION").get(0);
        Requirement language = requirementRepository.findByRequirementKey("CA_FSWP.LANGUAGE_ABILITY").get(0);

        assertThat(ca.getCompositionLogic())
                .contains("\"requirementId\":" + workExperience.getId())
                .contains("\"requirementId\":" + education.getId())
                .contains("\"requirementId\":" + language.getId());
    }
}
