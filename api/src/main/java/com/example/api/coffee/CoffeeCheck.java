package com.example.api.coffee;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coffee_check",
       uniqueConstraints = {
        @UniqueConstraint(columnNames = {"check_date", "task_key"})
    })
public class CoffeeCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="check_date", nullable=false)
    private LocalDate date;

    @Column(name="task_key", nullable=false, length=64)
    private String taskKey;

    @Column(nullable=false)
    private boolean done;

    private Integer doneByEmployeeId;
    private LocalDateTime doneAt;

    public CoffeeCheck() {}

    public CoffeeCheck(LocalDate date, String taskKey, boolean done) {
        this.date = date;
        this.taskKey = taskKey;
        this.done = done;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }

    public void setDate(LocalDate date) { this.date = date; }

    public String getTaskKey() { return taskKey; }

    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }

    public boolean isDone() { return done; }

    public void setDone(boolean done) { this.done = done; }

    public Integer getDoneByEmployeeId() { return doneByEmployeeId; }

    public void setDoneByEmployeeId(Integer doneByEmployeeId) { this.doneByEmployeeId = doneByEmployeeId; }

    public LocalDateTime getDoneAt() { return doneAt; }

    public void setDoneAt(LocalDateTime doneAt) { this.doneAt = doneAt; }
}
