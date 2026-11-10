package com.example.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller providing endpoints for manually testing reminder functionality.
 *
 * SEE README FOR INSTRUCTIONS ON HOW TO TEST THE REMINDERS!!!
 */
@RestController
@RequestMapping("/reminders")
public class ReminderTestController {

    private final ReminderScheduler scheduler;

    public ReminderTestController(ReminderScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/test")
    @Operation(
        summary = "Trigger reminders manually",
        description = "Manual test endpoint to trigger birthday, anniversary and plant care reminders. daysAhead controls the target date for birthday/anniversary reminders."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Reminders triggered successfully. Returns plain text message.",
            content = @Content(mediaType = "text/plain")
        )
    })
    public ResponseEntity<String> testReminders(
        @RequestParam(defaultValue = "7") int daysAhead
    ) {
        LocalDate targetDate = LocalDate.now().plusDays(daysAhead);

        // birthdays and anniversaries are triggered for a future date
        scheduler.triggerBirthdayReminderManual(targetDate);
        scheduler.triggerAnniversaryReminderManual(targetDate);

        // plant care is triggered for the current week
        scheduler.triggerPlantCareReminderManual(LocalDate.now());

        return ResponseEntity.ok(
            "Reminders triggered. Birthday/Anniversary target date: " + targetDate
        );
    }
}
