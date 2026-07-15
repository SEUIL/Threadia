package com.threadia.backend.sprint.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record SprintUpdateRequest(@NotBlank @Size(max=150) String name, String goal, LocalDate startDate, LocalDate endDate) {}
