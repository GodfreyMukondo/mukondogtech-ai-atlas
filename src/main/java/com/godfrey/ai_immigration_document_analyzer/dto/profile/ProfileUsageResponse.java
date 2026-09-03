package com.godfrey.ai_immigration_document_analyzer.dto.profile;

public record ProfileUsageResponse(

        long analysesUsed,

        long analysesLimit,

        long remainingAnalyses,

        double usagePercentage,

        boolean unlimited

) {
}