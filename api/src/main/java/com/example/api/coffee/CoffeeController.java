package com.example.api.coffee;

import com.example.api.Odoo;
import com.example.api.dart.DartPlayer;
import com.example.api.employee.Employee;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/coffee")
@CrossOrigin(origins = "*")
public class CoffeeController {

    @Autowired
    private CoffeeCheckRepository coffeeCheckRepository;

    private static final ZoneId zone = ZoneId.of("Europe/Berlin");

    // DTO used only for UI response
    record CoffeeTaskStatus(
        String taskKey,
        String group,
        String label,
        LocalDate lastDoneDate,
        Integer dueInDays
    ) {}

    private LocalDate parseAndValidateDate(String dateString) {
        if(dateString == null || dateString.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please select a valid date (use the date picker)");
        }

        final LocalDate date;
        try {
            date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please select a valid date (use the date picker)");
        }
        return date;
    }

    @GetMapping("/tasks/status")
    @Operation(
        summary = "Get coffee task statuses",
        description = "Returns all coffee tasks with last done date and (for biweekly tasks) days until due."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public List<CoffeeTaskStatus> getTaskStatuses() {

        LocalDate today = LocalDate.now();
        List<CoffeeTaskStatus> result = new ArrayList<>();

        for (CoffeeTask task : CoffeeTask.values()) {

            Optional<CoffeeCheck> lastDoneCheck =
                coffeeCheckRepository.findTopByTaskKeyAndDoneTrueOrderByDateDesc(task.name());

            LocalDate lastDoneDate = lastDoneCheck
                .map(CoffeeCheck::getDate)
                .orElse(null);

            Integer dueInDays = null;

            if ("BIWEEKLY".equals(task.group)) {
                if (lastDoneDate == null) {
                    dueInDays = 0; // never done, due today
                } else {
                    long daysSince = ChronoUnit.DAYS.between(lastDoneDate, today);
                    dueInDays = (int) Math.max(0, 14 - daysSince);
                }
            }

            result.add(new CoffeeTaskStatus(
                task.name(),
                task.group,
                task.label,
                lastDoneDate,
                dueInDays
            ));
        }

        return result;
    }


    /**
     * returns the employee responsible for the coffee machine today,
     * using a daily rotation method from class Odoo
     *
     * @return
     */
    @GetMapping("/duty/current")
    @Operation(
        summary = "Get current coffee duty",
        description = "Returns the employee responsible for coffee duty today."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Employee getCurrentCoffeeDuty() {
        try {
            Odoo.connectToOdoo();
            Odoo.login();
            return Odoo.getCoffeeDutyOfDate(LocalDate.now(zone));
        } catch (Exception e) {
            return new Employee(1,
                "CoffeeDuty",
                "coffee.duty@example.com",
                LocalDate.of(1,1,1),
                LocalDate.of(1,1,1)
            );
        }
    }

    /**
     * returns the employee responsible for the coffee machine tomorrow,
     * using a daily rotation method from class Odoo
     *
     * @return
     */
    @GetMapping("/duty/next")
    @Operation(
        summary = "Get next coffee duty",
        description = "Returns the employee responsible for coffee duty tomorrow."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public Employee getNextCoffeeDuty() {
        try {
            Odoo.connectToOdoo();
            Odoo.login();
            return Odoo.getCoffeeDutyOfDate(LocalDate.now(zone).plusDays(1));
        } catch (Exception e) {
            return new Employee(
                1,
                "CoffeeDuty",
                "coffee.duty@example.com",
                LocalDate.of(1, 1, 1),
                LocalDate.of(1, 1, 1)
            );
        }
    }

    /**
     * returns all stored coffee checklist entries for the current day,
     * tasks that aren't returned are considered not yet completed
     *
     * @return
     */
    @GetMapping("/checklist/current")
    @Operation(
        summary = "Get today's coffee checklist entries",
        description = "Returns all stored checklist entries for today. Tasks not returned are considered not completed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public List<CoffeeCheck> getChecklistToday() {
        try {
            return coffeeCheckRepository.findByDate(LocalDate.now());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * returns all stored coffee checklist entries for a specific date
     *
     * @param date the date for which the checklist entries are returned
     * @return
     */
    @GetMapping("/checklist/{date}")
    @Operation(
        summary = "Get coffee checklist entries by date",
        description = "Returns all stored checklist entries for the given date."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = CoffeeCheck.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date format. Returns plain text error message.",
            content = @Content(mediaType = "text/plain")
        )
    })
    public ResponseEntity<?> getChecklistByDate(@PathVariable String date) {
        try {
            return ResponseEntity.ok(coffeeCheckRepository.findByDate(LocalDate.parse(date)));
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body("Please select a valid date (use the date picker)");
        }
    }

    /**
     * creates or updates a checklist entry for a specific task and date;
     * if an entry already exists, its state is updated; otherwise a new entry is created
     *
     * @param requestBody contains the task key, the target date (optional),
     *                    the completion state (done is true/false),
     *                    and the employee who completed the task (optional)
     * @return
     */
    @PutMapping("/checklist/set")
    @Operation(
        summary = "Create or update a coffee checklist entry",
        description = "Creates a new entry or updates an existing one for the given taskKey and date. If date is missing, today is used."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Entry created/updated successfully",
            content = @Content(schema = @Schema(implementation = CoffeeCheck.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request (missing or blank taskKey)",
            content = @Content(mediaType = "text/plain")
        )
    })
    public CoffeeCheck setChecklist(@RequestBody CoffeeCheck requestBody) {

        LocalDate date;

        if( requestBody.getDate() != null) {
            date = requestBody.getDate();
        } else {
            date = LocalDate.now();
        }

        String taskKey = requestBody.getTaskKey();

        if (taskKey == null || taskKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskKey is required.");
        }

        Optional<CoffeeCheck> existing =
            coffeeCheckRepository.findByDateAndTaskKey(date, taskKey);

        CoffeeCheck check = existing.orElseGet(
            () -> new CoffeeCheck(date, taskKey, false));

        check.setDone(requestBody.isDone());
        check.setDoneAt(LocalDateTime.now());
        check.setDoneByEmployeeId(requestBody.getDoneByEmployeeId());

        return coffeeCheckRepository.save(check);
    }
}
