package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * REGULATORY VERSION ADMIN SERVICE
 * ============================================================================
 *
 * Administrator-only authoring surface for {@code RegulatoryVersion} - the
 * one entity every seeded/authored Requirement must reference
 * ({@code Requirement.regulatoryVersionId} is {@code NOT NULL}).
 *
 * Deliberately append-only, matching the entity's own design: there is no
 * update method here. Recording a new version for a regulation already on
 * file automatically closes the prior open-ended version's
 * {@code effectiveTo} and links the {@code supersedes}/{@code supersededBy}
 * chain - this is what "never silently overwrite historical rules" means
 * in practice; the old row's content is untouched, only its own window is
 * closed.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class RegulatoryVersionAdminService {

    private final RegulatoryVersionRepository regulatoryVersionRepository;

    @Transactional
    public RegulatoryVersionResponse create(RegulatoryVersionCreateRequest request) {

        RegulatoryVersion.RegulatoryVersionBuilder builder = RegulatoryVersion.builder()
                .regulationIdentity(request.getRegulationIdentity())
                .jurisdiction(request.getJurisdiction())
                .sourceType(request.getSourceType())
                .sourceAuthority(request.getSourceAuthority())
                .sourceReference(request.getSourceReference())
                .publicationDate(request.getPublicationDate())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .verificationStatus(request.getVerificationStatus())
                .changeSummary(request.getChangeSummary());

        RegulatoryVersion previous = regulatoryVersionRepository
                .findByRegulationIdentityAndEffectiveToIsNull(request.getRegulationIdentity())
                .orElse(null);

        RegulatoryVersion saved = regulatoryVersionRepository.save(builder.build());

        if (previous != null) {

            saved.setSupersedesVersionId(previous.getId());
            saved = regulatoryVersionRepository.save(saved);

            previous.setSupersededByVersionId(saved.getId());

            if (previous.getEffectiveTo() == null) {
                previous.setEffectiveTo(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDateTime.now());
            }

            regulatoryVersionRepository.save(previous);
        }

        return RegulatoryVersionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RegulatoryVersionResponse> listAll() {

        return regulatoryVersionRepository.findAll().stream()
                .map(RegulatoryVersionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RegulatoryVersionResponse getById(Long id) {

        return regulatoryVersionRepository.findById(id)
                .map(RegulatoryVersionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Regulatory version not found."));
    }
}
