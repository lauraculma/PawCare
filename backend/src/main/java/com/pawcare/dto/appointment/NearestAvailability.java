package com.pawcare.dto.appointment;

import java.time.LocalDate;

public record NearestAvailability(LocalDate date, String time, String veterinarianCedula,
                                  String veterinarianName) { }
