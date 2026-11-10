package com.example.api;

import com.example.api.employee.Employee;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReminderSchedulerTest {

    private Mattermost mattermost;
    private ReminderScheduler scheduler;

    @Before
    public void setUp() {
        mattermost = Mockito.mock(Mattermost.class);
        scheduler = new ReminderScheduler(mattermost);
    }

    @Test
    public void birthdayReminder_sendsWhenEmployeesExistForTargetDate() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        LocalDate target = fixedNow.plusDays(7);

        Employee e1 = mock(Employee.class);
        Employee e2 = mock(Employee.class);
        when(e1.getName()).thenReturn("Alice");
        when(e2.getName()).thenReturn("Bob");

        List<Employee> employees = List.of(e1, e2);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            // prevent real Odoo calls
            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getEmployeesWithBirthdayOn(target)).thenReturn(employees);

            scheduler.sendNextBirthdayReminder();

            String expected =
                "Erinnerung: In einer Woche (am " + target + ") ist der Geburtstag von Alice, Bob. Bitte an Kuchen denken!";

            verify(mattermost, times(1)).sendToChannel(expected);
            verifyNoMoreInteractions(mattermost);
        }
    }

    @Test
    public void birthdayReminder_noEmployees_noSend() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        LocalDate target = fixedNow.plusDays(7);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getEmployeesWithBirthdayOn(target)).thenReturn(Collections.emptyList());

            scheduler.sendNextBirthdayReminder();

            verifyNoInteractions(mattermost);
        }
    }

    @Test
    public void anniversaryReminder_sendsWhenEmployeesExistForTargetDate() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        LocalDate target = fixedNow.plusDays(7);

        Employee e1 = mock(Employee.class);
        when(e1.getName()).thenReturn("Max Mustermann");

        List<Employee> employees = List.of(e1);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getEmployeesWithAnniversaryOn(target)).thenReturn(employees);

            scheduler.sendNextAnniversaryReminder();

            String expected =
                "Erinnerung: In einer Woche (" + target + ") ist das Firmenjubiläum von Max Mustermann. Bitte an Kuchen denken!";

            verify(mattermost, times(1)).sendToChannel(expected);
            verifyNoMoreInteractions(mattermost);
        }
    }

    @Test
    public void anniversaryReminder_noEmployees_noSend() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        LocalDate target = fixedNow.plusDays(7);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getEmployeesWithAnniversaryOn(target)).thenReturn(Collections.emptyList());

            scheduler.sendNextAnniversaryReminder();

            verifyNoInteractions(mattermost);
        }
    }

    @Test
    public void plantCareReminder_workMailPresent_sendsDirectMessage() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        int week = fixedNow.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        Employee carer = mock(Employee.class);
        when(carer.getName()).thenReturn("Ana Muller");
        when(carer.getWorkMail()).thenReturn("ana.muller@example.com");

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getPlantCarerOfWeek(week)).thenReturn(carer);

            scheduler.sendPlantCareReminder();

            String expected =
                "Erinnerung: Diese Woche ist Ana Muller für die Pflanzenpflege zuständig. Bitte an das Gießen denken!";

            verify(mattermost, times(1))
                .sendDirectMessage("ana.muller@example.com", expected);
            verify(mattermost, never()).sendToChannel(anyString());
            verifyNoMoreInteractions(mattermost);
        }
    }

    @Test
    public void plantCareReminder_noWorkMail_sendsToChannel() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        int week = fixedNow.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        Employee carer = mock(Employee.class);
        when(carer.getName()).thenReturn("Ana Muller");
        when(carer.getWorkMail()).thenReturn(null);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getPlantCarerOfWeek(week)).thenReturn(carer);

            scheduler.sendPlantCareReminder();

            String expected =
                "Erinnerung: Diese Woche ist Ana Muller für die Pflanzenpflege zuständig. Bitte an das Gießen denken!";

            verify(mattermost, times(1)).sendToChannel(expected);
            verify(mattermost, never()).sendDirectMessage(anyString(), anyString());
            verifyNoMoreInteractions(mattermost);
        }
    }

    @Test
    public void plantCareReminder_noCarer_noSend() {
        LocalDate fixedNow = LocalDate.of(2026, 1, 1);
        int week = fixedNow.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        try (MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class);
             MockedStatic<Odoo> odooMock = Mockito.mockStatic(Odoo.class)) {

            localDateMock.when(LocalDate::now).thenReturn(fixedNow);

            odooMock.when(Odoo::connectToOdoo).thenAnswer(inv -> null);
            odooMock.when(Odoo::login).thenAnswer(inv -> null);

            odooMock.when(() -> Odoo.getPlantCarerOfWeek(week)).thenReturn(null);

            scheduler.sendPlantCareReminder();

            verifyNoInteractions(mattermost);
        }
    }
}

