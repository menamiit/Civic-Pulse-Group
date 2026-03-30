package com.menamiit.smartcityproject.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "grievances")
@Data
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category = ComplaintCategory.OTHER;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column
    private String location;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievanceStatus status = GrievanceStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievancePriority priority = GrievancePriority.MEDIUM;

    @Column
    private LocalDate dueDate;

    @Column(nullable = false)
    private LocalDateTime statusUpdatedAt = LocalDateTime.now();

    @Column
    private Integer citizenRating;

    @Column(length = 2000)
    private String citizenFeedback;

    @Column(length = 1000)
    private String lowRatingReason;

    @Column(nullable = false)
    private boolean citizenReopened = false;

    @Column(length = 2000)
    private String reopenReason;

    @Column
    private LocalDateTime reopenedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    @ManyToOne
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Department getMappedDepartment() {
        return category == null ? Department.GENERAL : category.mappedDepartment();
    }
}
