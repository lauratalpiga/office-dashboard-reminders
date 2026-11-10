package com.example.api;

import com.example.api.employee.Employee;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schedules and triggers Mattermost reminders.
 *
 * SEE README FOR INSTRUCTIONS ON HOW TO TEST THE REMINDERS!!!
 */

@Component
public class ReminderScheduler {
    private final Mattermost mattermost;

    @Value("${plantcare.test-user:}")
    private String plantCareTestUser;

    public ReminderScheduler(Mattermost mattermost) {
        this.mattermost = mattermost;
    }

    // ======================================================
    // Scheduled reminders (triggered automatically by cron)
    // ======================================================

    // runs every day at 8:05
    @Scheduled(cron = "0 5 8 * * *", zone = "Europe/Berlin")
    public void sendNextBirthdayReminder() {

        try {
            Odoo.connectToOdoo();
            Odoo.login();

            LocalDate targetDate = LocalDate.now().plusDays(7);

            List<Employee> employees = Odoo.getEmployeesWithBirthdayOn(targetDate);

            if (employees.isEmpty()) return;

            String names = employees.stream()
                .map(Employee::getName)
                .collect(Collectors.joining(", "));

            mattermost.sendToChannel("Erinnerung: In einer Woche (am " + targetDate + ") ist der Geburtstag von " + names +
                ". Bitte an Kuchen denken!");

        } catch (XmlRpcException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // runs every day at 8:10
    @Scheduled(cron = "0 10 8 * * *", zone = "Europe/Berlin")
    public void sendNextAnniversaryReminder() {

        try {
            Odoo.connectToOdoo();
            Odoo.login();

            LocalDate targetDate = LocalDate.now().plusDays(7);

            List<Employee> employees = Odoo.getEmployeesWithAnniversaryOn(targetDate);
            if (employees.isEmpty()) return;

            String names = employees.stream()
                .map(Employee::getName)
                .collect(Collectors.joining(", "));

            mattermost.sendToChannel(
                "Erinnerung: In einer Woche (" + targetDate + ") ist das Firmenjubiläum von " + names +
                    ". Bitte an Kuchen denken!");

        } catch (XmlRpcException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // runs every Monday at 8:00
    @Scheduled(cron = "0 0 8 * * MON", zone = "Europe/Berlin")
    public void sendPlantCareReminder() {

        try {
            Odoo.connectToOdoo();
            Odoo.login();

            // finding the current week
            int week = LocalDate.now(ZoneId.of("Europe/Berlin"))
                .get(WeekFields.ISO.weekOfWeekBasedYear());

            Employee currentCarer = Odoo.getPlantCarerOfWeek(week);

            if (currentCarer == null) {
                return;
            }

            String message = String.format("Erinnerung: Diese Woche ist %s für die Pflanzenpflege zuständig. Bitte an das Gießen denken!",
                currentCarer.getName());

            // override for tutor testing
            if (plantCareTestUser != null && !plantCareTestUser.isBlank()) {
                mattermost.sendDirectMessage(plantCareTestUser, message);
                return;
            }

            String workMail = currentCarer.getWorkMail();

            if(workMail != null && !workMail.isBlank()) {
                // sending reminder to the carer of the week
                mattermost.sendDirectMessage(workMail, message);
            } else {
                // if we couldn't find an email, sent it to channel Stuttgart
                mattermost.sendToChannel(message);
            }

        } catch (XmlRpcException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================================================
    // Manual reminder triggers (used for testing via REST endpoint)
    // ======================================================

    public void triggerBirthdayReminderManual(LocalDate targetDate) {
        try {
            Odoo.connectToOdoo();
            Odoo.login();

            List<Employee> employees = Odoo.getEmployeesWithBirthdayOn(targetDate);
            if (employees.isEmpty()) {
                System.out.println("No birthdays for " + targetDate);
                return;
            }

            String names = employees.stream()
                .map(Employee::getName)
                .collect(Collectors.joining(", "));

            String message =
                "[TEST] Erinnerung: In einer Woche (am " + targetDate +
                    ") ist der Geburtstag von " + names + ".";

            mattermost.sendToChannel(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void triggerAnniversaryReminderManual(LocalDate targetDate) {
        try {
            Odoo.connectToOdoo();
            Odoo.login();

            List<Employee> employees = Odoo.getEmployeesWithAnniversaryOn(targetDate);
            if (employees.isEmpty()) {
                System.out.println("No anniversaries for " + targetDate);
                return;
            }

            String names = employees.stream()
                .map(Employee::getName)
                .collect(Collectors.joining(", "));

            String message =
                "[TEST] Erinnerung: In einer Woche (" + targetDate +
                    ") ist das Firmenjubiläum von " + names + ".";

            mattermost.sendToChannel(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void triggerPlantCareReminderManual(LocalDate date) {
        try {
            Odoo.connectToOdoo();
            Odoo.login();

            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            Employee currentCarer = Odoo.getPlantCarerOfWeek(week);

            if (currentCarer == null) {
                System.out.println("No plant carer for week " + week);
                return;
            }

            String message = String.format(
                "[TEST] Erinnerung: Diese Woche ist %s für die Pflanzenpflege zuständig. Bitte an das Gießen denken!",
                currentCarer.getName()
            );

            // override for tutor testing (same behavior as scheduled method)
            if (plantCareTestUser != null && !plantCareTestUser.isBlank()) {
                mattermost.sendDirectMessage(plantCareTestUser, message);
                return;
            }

            String workMail = currentCarer.getWorkMail();

            if (workMail != null && !workMail.isBlank()) {
                mattermost.sendDirectMessage(workMail, message);
            } else {
                mattermost.sendToChannel(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
