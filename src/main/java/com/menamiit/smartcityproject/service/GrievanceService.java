package com.menamiit.smartcityproject.service;

import com.menamiit.smartcityproject.entity.ComplaintCategory;
import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.Grievance;
import com.menamiit.smartcityproject.entity.GrievancePriority;
import com.menamiit.smartcityproject.entity.GrievanceStatus;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.repository.GrievanceRepository;
import com.menamiit.smartcityproject.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        return fileGrievance(citizenUsername, title, ComplaintCategory.OTHER, description, null, null, null, null);
    }

    public Grievance fileGrievance(
        String citizenUsername,
        String title,
        ComplaintCategory category,
        String description,
        String location,
        Double latitude,
        Double longitude,
        String photoPath
    ) {
        User citizen = userRepository.findByUsername(citizenUsername)
            .orElseThrow(() -> new IllegalArgumentException("Citizen not found"));

        Grievance grievance = new Grievance();
        grievance.setTitle(title);
        grievance.setCategory(category);
        grievance.setDescription(description);
        grievance.setLocation(location);
        grievance.setLatitude(latitude);
        grievance.setLongitude(longitude);
        grievance.setPhotoPath(photoPath);
        grievance.setCitizen(citizen);
        grievance.setStatus(GrievanceStatus.PENDING);
        grievance.setStatusUpdatedAt(LocalDateTime.now());
        return grievanceRepository.save(grievance);
    }

    public List<Grievance> getCitizenGrievances(String citizenUsername) {
        return grievanceRepository.findByCitizenUsername(citizenUsername);
    }

    public List<Grievance> getOfficerGrievances(String officerUsername) {
        return grievanceRepository.findByAssignedOfficerUsername(officerUsername);
    }

    public List<Grievance> getAllGrievances() {
        return grievanceRepository.findAllByOrderByCreatedAtDesc();
    }

    public Grievance assignToOfficer(Long grievanceId, Long officerId) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));
        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new IllegalArgumentException("Officer not found"));

        if (officer.getRole() != UserRole.OFFICER) {
            throw new IllegalArgumentException("Selected user is not an officer");
        }
        if (!officer.isVerified()) {
            throw new IllegalArgumentException("Officer account is not verified");
        }

        Department grievanceDepartment = grievance.getMappedDepartment();
        if (officer.getDepartment() != grievanceDepartment) {
            throw new IllegalArgumentException("Officer does not belong to grievance department");
        }

        grievance.setAssignedOfficer(officer);
        return grievanceRepository.save(grievance);
    }

    public Grievance updateAdminMetadata(Long grievanceId, GrievancePriority priority, LocalDate dueDate) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (priority == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        grievance.setPriority(priority);
        grievance.setDueDate(dueDate);
        return grievanceRepository.save(grievance);
    }

    public Grievance updateStatus(Long grievanceId, GrievanceStatus status, String officerUsername) {
        return updateStatus(grievanceId, status, officerUsername, null, List.of());
    }

    public Grievance updateStatus(
        Long grievanceId,
        GrievanceStatus status,
        String officerUsername,
        String resolutionNotes,
        List<String> resolutionImagePaths
    ) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (grievance.getAssignedOfficer() == null || !grievance.getAssignedOfficer().getUsername().equals(officerUsername)) {
            throw new IllegalArgumentException("You can only update your assigned grievances");
        }

        if (grievance.getStatus() == GrievanceStatus.RESOLVED && status != GrievanceStatus.RESOLVED) {
            throw new IllegalArgumentException("Resolved grievances cannot be moved back to active states");
        }

        if (status == GrievanceStatus.RESOLVED) {
            if (resolutionNotes == null || resolutionNotes.isBlank()) {
                throw new IllegalArgumentException("Resolution notes are required when marking a grievance as resolved");
            }
            grievance.setResolutionNotes(resolutionNotes.trim());

            List<String> safePaths = resolutionImagePaths == null ? List.of() : resolutionImagePaths;
            if (safePaths.size() > 5) {
                throw new IllegalArgumentException("A maximum of 5 resolution images can be uploaded");
            }
            if (!safePaths.isEmpty()) {
                grievance.setResolutionImagePaths(String.join("\n", safePaths));
            }
        }

        grievance.setStatus(status);
        grievance.setStatusUpdatedAt(LocalDateTime.now());
        return grievanceRepository.save(grievance);
    }
}
