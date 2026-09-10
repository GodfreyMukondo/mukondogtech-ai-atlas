package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceGraphService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * Wraps {@link EvidenceGraphService#graphForRequirementEvaluation} - the
 * same already-authorized graph {@code EvidenceGraphController} exposes.
 * Authorization happens entirely inside that call; this tool adds nothing
 * beyond typed input/output and a stable name.
 */
@Component
@RequiredArgsConstructor
public class EvidenceGraphContextTool implements AgentTool<EvidenceGraphContextTool.Input, EvidenceGraphResponse> {

    private final EvidenceGraphService evidenceGraphService;

    @Override
    public AgentToolName name() {
        return AgentToolName.GET_EVIDENCE_GRAPH_CONTEXT;
    }

    @Override
    public AgentToolAccessLevel accessLevel() {
        return AgentToolAccessLevel.READ_ONLY;
    }

    @Override
    public EvidenceGraphResponse invoke(AuthenticatedUser actor, Input input) {
        return evidenceGraphService.graphForRequirementEvaluation(actor, input.evaluationId());
    }

    public record Input(
            Long evaluationId
    ) {
    }
}
