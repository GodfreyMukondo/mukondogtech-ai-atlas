package com.godfrey.ai_immigration_document_analyzer.service;

import org.springframework.stereotype.Service;

@Service
public class ApprovalPredictionService {

    public double predict(
            double funds,
            double gpa,
            int passportMonths
    ) {

        double score =
                (funds * 0.4)
                        + (gpa * 20)
                        + (passportMonths * 2);

        return Math.min(score / 100, 1.0);
    }
}