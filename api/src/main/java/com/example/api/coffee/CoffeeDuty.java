package com.example.api.coffee;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "coffee_duty")
public class CoffeeDuty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="duty_date", nullable=false, unique = true)
    private LocalDate date;

    @Column(name="employee_id", nullable=false)
    private int employeeId;

    public CoffeeDuty() {}

    public CoffeeDuty(LocalDate date, int employeeId) {
        this.date = date;
        this.employeeId = employeeId;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getEmployeeId() { return employeeId; }

    public void setDate(LocalDate date) { this.date = date; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

}
