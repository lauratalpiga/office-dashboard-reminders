package com.example.api.coffee;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface CoffeeDutyRepository extends JpaRepository<CoffeeDuty, Long> {
    Optional<CoffeeDuty> findByDate(LocalDate date);
}
