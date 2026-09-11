package com.jobsignal.ai.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface JobRepository extends JpaRepository<Job, Long> {
    boolean existsByCompanyIdAndExternalJobId(Long companyId, String externalJobId);

    @Modifying
    @Query("UPDATE Job j SET j.lastSeenAt = :now WHERE j.company.id = :companyId AND j.externalJobId = :externalJobId")
    void touchLastSeenAt(@Param("companyId") Long companyId,
                         @Param("externalJobId") String externalJobId,
                         @Param("now") Instant now);
}
