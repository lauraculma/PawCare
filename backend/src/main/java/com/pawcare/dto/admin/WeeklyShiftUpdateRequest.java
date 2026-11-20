package com.pawcare.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record WeeklyShiftUpdateRequest(@NotNull List<@Valid DayShift> shifts) {
    public record DayShift(
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startTime
    ) { }
}
