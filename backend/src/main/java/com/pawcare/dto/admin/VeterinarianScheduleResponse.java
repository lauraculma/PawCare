package com.pawcare.dto.admin;

import com.pawcare.dto.auth.UserResponse;

import java.time.LocalTime;
import java.util.List;

public record VeterinarianScheduleResponse(UserResponse veterinarian, List<DayShift> shifts) {
    public record DayShift(int dayOfWeek, LocalTime startTime) { }
}
