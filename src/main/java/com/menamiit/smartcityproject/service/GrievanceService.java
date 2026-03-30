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
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (grievance.getAssignedOfficer() == null || !grievance.getAssignedOfficer().getUsername().equals(officerUsername)) {
            throw new IllegalArgumentException("You can only update your assigned grievances");
        }

        if (grievance.getStatus() == GrievanceStatus.RESOLVED) {
            throw new IllegalArgumentException("Resolved grievances cannot be updated by officer unless reopened by citizen");
        }

        if (status == GrievanceStatus.REOPENED) {
            throw new IllegalArgumentException("Only citizen can reopen a grievance");
        }

        grievance.setStatus(status);
        grievance.setStatusUpdatedAt(LocalDateTime.now());
        return grievanceRepository.save(grievance);
    }

    public Grievance addCitizenFeedback(
        Long grievanceId,
        String citizenUsername,
        Integer rating,
        String feedback,
        String lowRatingReason
    ) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (grievance.getCitizen() == null || !grievance.getCitizen().getUsername().equals(citizenUsername)) {
            throw new IllegalArgumentException("You can only rate your own grievances");
        }
        if (grievance.getStatus() != GrievanceStatus.RESOLVED) {
            throw new IllegalArgumentException("Feedback can be submitted only for resolved grievances");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        grievance.setCitizenRating(rating);
        grievance.setCitizenFeedback(normalizeOptionalText(feedback));

        if (rating <= 2) {
            grievance.setLowRatingReason(normalizeOptionalText(lowRatingReason));
        } else {
            grievance.setLowRatingReason(null);
        }

        return grievanceRepository.save(grievance);
    }

    public Grievance reopenByCitizen(Long grievanceId, String citizenUsername, String reopenReason) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
            .orElseThrow(() -> new IllegalArgumentException("Grievance not found"));

        if (grievance.getCitizen() == null || !grievance.getCitizen().getUsername().equals(citizenUsername)) {
            throw new IllegalArgumentException("You can only reopen your own grievances");
        }
        if (grievance.getStatus() != GrievanceStatus.RESOLVED) {
            throw new IllegalArgumentException("Only resolved grievances can be reopened");
        }
        if (grievance.isCitizenReopened()) {
            throw new IllegalArgumentException("A grievance can be reopened by citizen only once");
        }

        String normalizedReason = normalizeOptionalText(reopenReason);
        if (normalizedReason == null) {
            throw new IllegalArgumentException("Reason is required to reopen a grievance");
        }

        grievance.setStatus(GrievanceStatus.REOPENED);
        grievance.setStatusUpdatedAt(LocalDateTime.now());
        grievance.setCitizenReopened(true);
        grievance.setReopenReason(normalizedReason);
        grievance.setReopenedAt(LocalDateTime.now());

        grievance.setCitizenRating(null);
        grievance.setCitizenFeedback(null);
        grievance.setLowRatingReason(null);

        return grievanceRepository.save(grievance);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
