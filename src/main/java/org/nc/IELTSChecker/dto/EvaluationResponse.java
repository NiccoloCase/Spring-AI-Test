package org.nc.IELTSChecker.dto;

import java.util.Map;

public record EvaluationResponse(
        double taskResponse,
        double coherenceCohesion,
        double lexicalResource,
        double grammaticalRangeAccuracy,
        double overallBand,
        String examinerFeedback,
        Map<String, String> suggestions
) {}