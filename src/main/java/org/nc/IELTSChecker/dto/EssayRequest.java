package org.nc.IELTSChecker.dto;

public record EssayRequest(
        String question,
        String essay,
        String taskType
) {}