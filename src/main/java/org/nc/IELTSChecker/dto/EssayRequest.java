package org.nc.IELTSChecker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EssayRequest(
        @NotNull(message = "Question cannot be null")
        @NotEmpty(message = "Question cannot be empty")
        @Size(min = 10, max = 2000, message = "Question must be between 10 and 200 characters")
        String question,

        @NotNull(message = "Essay content cannot be null")
        @NotEmpty(message = "Essay content cannot be empty")
        @Size(min = 100, max = 6000, message = "Essay content must be between 100 and 1000 characters")
        String essay,
        String taskType
) {}