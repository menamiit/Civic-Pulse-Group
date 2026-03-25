package com.menamiit.smartcityproject.service;

import com.menamiit.smartcityproject.entity.Grievance;
import com.menamiit.smartcityproject.entity.GrievanceStatus;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.repository.GrievanceRepository;
import com.menamiit.smartcityproject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;

    public GrievanceService(GrievanceRepository grievanceRepository, UserRepository userRepository) {
        this.grievanceRepository = grievanceRepository;
        this.userRepository = userRepository;
    }

    public Grievance fileGrievance(String citizenUsername, String title, String description) {
        User citizen = userRepository.findByUsername(citizenUsername)
            .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));

        Grievance grievance = new Grievance();
        grievance.setTitle(title);
        grievance.setDescription(description);
        grievance.setCitizen(citizen);
        grievance.setStatus(GrievanceStatus.NEW);
        return grievanceRepository.save(grievance);
    }

    public List<Grievance> getCitizenGrievances(String citizenUsername) {
        return grievanceRepository.findByCitizenUsername(citizenUsername);
    }

    public List<Grievance> getOfficerGrievances(String officerUsername) {
        return grievanceRepository.findByAssignedOfficerUsername(officerUsername);
    }

    public List<Grievance> getAllGrievances() {
        return grievanceRepository.findAll();
    }

    public Grievance assignToOfficer(Long grievanceId, Long officerId) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));
        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new IllegalArgumentException("Officer not found"));

        if (officer.getRole() != UserRole.OFFICER) {
            throw new IllegalArgumentException("Selected user is not an officer");
        }

        grievance.setAssignedOfficer(officer);
        grievance.setStatus(GrievanceStatus.ASSIGNED);
        return grievanceRepository.save(grievance);
    }

    public Grievance updateStatus(Long grievanceId, GrievanceStatus status, String officerUsername) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (grievance.getAssignedOfficer() == null || !grievance.getAssignedOfficer().getUsername().equals(officerUsername)) {
            throw new IllegalArgumentException("You can only update your assigned grievances");
        }

        grievance.setStatus(status);
        return grievanceRepository.save(grievance);
    }
}
