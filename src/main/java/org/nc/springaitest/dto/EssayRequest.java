package org.nc.springaitest.dto;

public record EssayRequest(
        String question,
        String essay,
        String taskType
) {}