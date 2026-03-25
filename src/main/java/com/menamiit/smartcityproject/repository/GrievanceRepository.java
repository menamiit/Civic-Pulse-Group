package com.menamiit.smartcityproject.repository;

import com.menamiit.smartcityproject.entity.Grievance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findByCitizenUsername(String username);

    List<Grievance> findByAssignedOfficerUsername(String username);
}
