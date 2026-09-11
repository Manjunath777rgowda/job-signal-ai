package com.jobsignal.ai.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByName(String name);
    Optional<Company> findByNameIgnoreCase(String name);
    List<Company> findAllByActiveTrue();
}
