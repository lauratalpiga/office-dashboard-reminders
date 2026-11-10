package com.example.api.coffee;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CoffeeCheckRepository extends JpaRepository<CoffeeCheck, Long> {
    List<CoffeeCheck> findByDate(LocalDate date);
    Optional<CoffeeCheck> findByDateAndTaskKey(LocalDate date, String taskKey);

    // finds the last time a task was done (done=true), newest date first
    Optional<CoffeeCheck> findTopByTaskKeyAndDoneTrueOrderByDateDesc(String taskKey);
}
