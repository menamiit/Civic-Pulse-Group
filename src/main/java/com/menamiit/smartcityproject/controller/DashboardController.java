package com.menamiit.smartcityproject.controller;

import com.menamiit.smartcityproject.entity.ComplaintCategory;
import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.GrievanceStatus;
import com.menamiit.smartcityproject.entity.Grievance;
import com.menamiit.smartcityproject.entity.GrievancePriority;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.service.GrievanceService;
import com.menamiit.smartcityproject.service.FileStorageService;
import com.menamiit.smartcityproject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final GrievanceService grievanceService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public DashboardController(GrievanceService grievanceService, UserService userService, FileStorageService fileStorageService) {
        this.grievanceService = grievanceService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "redirect:/admin/home";
        }
        if (hasRole(authentication, "ROLE_OFFICER")) {
            return "redirect:/officer/home";
        }
        return "redirect:/citizen/home";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Grievance> roleGrievances;
        if (user.getRole() == UserRole.CITIZEN) {
            roleGrievances = grievanceService.getCitizenGrievances(user.getUsername());
        } else if (user.getRole() == UserRole.OFFICER) {
            roleGrievances = grievanceService.getOfficerGrievances(user.getUsername());
        } else {
            roleGrievances = grievanceService.getAllGrievances();
        }

        long solvedCount = roleGrievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .count();

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole());
        model.addAttribute("verified", user.isVerified());
        model.addAttribute("totalCount", roleGrievances.size());
        model.addAttribute("solvedCount", solvedCount);
        model.addAttribute("isCitizen", user.getRole() == UserRole.CITIZEN);
        model.addAttribute("isOfficer", user.getRole() == UserRole.OFFICER);
        model.addAttribute("isAdmin", user.getRole() == UserRole.ADMIN);
        return "profile";
    }

    @GetMapping("/citizen/home")
    public String citizenHome(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances =
            grievanceService.getCitizenGrievances(authentication.getName());

        long solvedCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .count();
        long openCount = grievances.size() - solvedCount;

        List<com.menamiit.smartcityproject.entity.Grievance> latestSubmitted = grievances.stream()
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getCreatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getUpdatedAt).reversed())
            .limit(5)
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("grievances", grievances);
        model.addAttribute("totalCount", grievances.size());
        model.addAttribute("solvedCount", solvedCount);
        model.addAttribute("openCount", openCount);
        model.addAttribute("latestSubmitted", latestSubmitted);
        model.addAttribute("latestSolved", latestSolved);
        return "citizen-home";
    }

    @GetMapping("/citizen/my-grievances")
    public String myGrievances(Authentication authentication, Model model) {
        List<Grievance> grievances = grievanceService.getCitizenGrievances(authentication.getName());

        List<Grievance> activeGrievances = grievances.stream()
            .filter(g -> g.getStatus() != GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(Grievance::getStatusUpdatedAt).reversed())
            .toList();

        List<Grievance> resolvedGrievances = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(Grievance::getStatusUpdatedAt).reversed())
            .limit(10)
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("activeGrievances", activeGrievances);
        model.addAttribute("resolvedGrievances", resolvedGrievances);
        model.addAttribute("activeCount", activeGrievances.size());
        model.addAttribute("resolvedCount", resolvedGrievances.size());
        return "citizen-my-grievances";
    }

    @PostMapping("/citizen/grievances/{id}/feedback")
    public String submitFeedback(
        Authentication authentication,
        @PathVariable Long id,
        @RequestParam Integer rating,
        @RequestParam(required = false) String feedback,
        @RequestParam(required = false) String lowRatingReason
    ) {
        try {
            grievanceService.addCitizenFeedback(id, authentication.getName(), rating, feedback, lowRatingReason);
            return "redirect:/citizen/my-grievances?feedbackSaved=true";
        } catch (Exception ex) {
            log.warn("Citizen feedback failed for grievance {} by {}: {}", id, authentication.getName(), ex.getMessage());
            return "redirect:/citizen/my-grievances?feedbackError=true";
        }
    }

    @PostMapping("/citizen/grievances/{id}/reopen")
    public String reopenGrievance(
        Authentication authentication,
        @PathVariable Long id,
        @RequestParam String reopenReason
    ) {
        try {
            grievanceService.reopenByCitizen(id, authentication.getName(), reopenReason);
            return "redirect:/citizen/my-grievances?reopened=true";
        } catch (Exception ex) {
            log.warn("Citizen reopen failed for grievance {} by {}: {}", id, authentication.getName(), ex.getMessage());
            return "redirect:/citizen/my-grievances?reopenError=true";
        }
    }

    @GetMapping("/citizen/complaints/new")
    public String newComplaint(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("categories", ComplaintCategory.values());
        return "citizen-complaint-new";
    }

    @PostMapping("/citizen/complaints")
    public String submitComplaint(
        Authentication authentication,
        @RequestParam String title,
        @RequestParam ComplaintCategory category,
        @RequestParam String description,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) MultipartFile photo,
        Model model
    ) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Logged in user not found in database"));

            if (user.getRole() != UserRole.CITIZEN) {
                throw new IllegalArgumentException("Only citizen accounts can submit complaints");
            }

            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Complaint title is required");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Complaint description is required");
            }

            String photoPath = null;
            try {
                photoPath = fileStorageService.storeComplaintPhoto(photo);
            } catch (Exception uploadEx) {
                // File upload is optional, so complaint submission should continue without photo.
                log.warn("Photo upload failed for user {}: {}", authentication.getName(), uploadEx.getMessage());
            }

            String normalizedLocation = (location == null || location.isBlank()) ? null : location.trim();

            grievanceService.fileGrievance(
                authentication.getName(),
                title,
                category,
                description,
                normalizedLocation,
                latitude,
                longitude,
                photoPath
            );
            return "redirect:/citizen/home";
        } catch (Exception ex) {
            log.error("Complaint submission failed for user {}", authentication.getName(), ex);
            model.addAttribute("username", authentication.getName());
            model.addAttribute("categories", ComplaintCategory.values());
            model.addAttribute("error", ex.getMessage() == null ? "Could not submit complaint. Please try again." : ex.getMessage());
            return "citizen-complaint-new";
        }
    }

    @PostMapping("/citizen/grievances")
    public String fileGrievance(
        Authentication authentication,
        @RequestParam String title,
        @RequestParam String description
    ) {
        grievanceService.fileGrievance(authentication.getName(), title, description);
        return "redirect:/citizen/home";
    }

    @GetMapping("/officer/home")
    public String officerHome(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances =
            grievanceService.getOfficerGrievances(authentication.getName());

        long solvedCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .count();
        long activeCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.PENDING
                || g.getStatus() == GrievanceStatus.IN_PROGRESS
                || g.getStatus() == GrievanceStatus.REOPENED)
            .count();

        List<com.menamiit.smartcityproject.entity.Grievance> latestAssigned = grievances.stream()
            .filter(g -> g.getStatus() != GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getStatusUpdatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getStatusUpdatedAt).reversed())
            .limit(5)
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("grievances", grievances);
        model.addAttribute("totalAssigned", grievances.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("solvedCount", solvedCount);
        model.addAttribute("latestAssigned", latestAssigned);
        model.addAttribute("latestSolved", latestSolved);
        model.addAttribute("statuses", GrievanceStatus.values());
        return "officer-home";
    }

    @GetMapping("/officer/grievances")
    public String officerGrievances(Authentication authentication, Model model) {
        List<Grievance> grievances = grievanceService.getOfficerGrievances(authentication.getName());

        List<Grievance> activeGrievances = grievances.stream()
            .filter(g -> g.getStatus() != GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(Grievance::getStatusUpdatedAt).reversed())
            .toList();

        List<Grievance> resolvedGrievances = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(Grievance::getStatusUpdatedAt).reversed())
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("activeGrievances", activeGrievances);
        model.addAttribute("resolvedGrievances", resolvedGrievances);
        model.addAttribute("statuses", GrievanceStatus.values());
        return "officer-grievances";
    }

    @PostMapping("/officer/grievances/{id}/status")
    public String updateStatus(
        Authentication authentication,
        @PathVariable Long id,
        @RequestParam GrievanceStatus status
    ) {
        try {
            grievanceService.updateStatus(id, status, authentication.getName());
            return "redirect:/officer/grievances?updated=true";
        } catch (Exception ex) {
            log.warn("Officer status update blocked for grievance {} by {}: {}", id, authentication.getName(), ex.getMessage());
            return "redirect:/officer/grievances?statusError=true";
        }
    }

    @GetMapping("/admin/home")
    public String adminHome(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances = grievanceService.getAllGrievances();
        List<User> users = userService.findAllUsers();

        long resolvedCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .count();
        long pendingCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.PENDING)
            .count();
        long inProgressCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.IN_PROGRESS || g.getStatus() == GrievanceStatus.REOPENED)
            .count();
        long unassignedCount = grievances.stream()
            .filter(g -> g.getAssignedOfficer() == null)
            .count();
        long highPriorityCount = grievances.stream()
            .filter(g -> g.getPriority() == GrievancePriority.HIGH)
            .count();
        long pendingOfficerVerification = users.stream()
            .filter(u -> u.getRole() == UserRole.OFFICER && !u.isVerified())
            .count();
        long overdueCount = grievances.stream()
            .filter(g -> g.getDueDate() != null
                && g.getDueDate().isBefore(LocalDate.now())
                && g.getStatus() != GrievanceStatus.RESOLVED)
            .count();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSubmitted = grievances.stream()
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getCreatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestResolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getStatusUpdatedAt).reversed())
            .limit(5)
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("totalGrievances", grievances.size());
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("unassignedCount", unassignedCount);
        model.addAttribute("highPriorityCount", highPriorityCount);
        model.addAttribute("pendingOfficerVerification", pendingOfficerVerification);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("latestSubmitted", latestSubmitted);
        model.addAttribute("latestResolved", latestResolved);
        return "admin-home";
    }

    @GetMapping("/admin/grievances")
    public String adminGrievances(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances = grievanceService.getAllGrievances();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("grievances", grievances);
        return "admin-grievances";
    }

    @GetMapping("/admin/grievances/manage")
    public String adminGrievanceManagement(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances = grievanceService.getAllGrievances();
        Map<Long, List<User>> eligibleOfficersByGrievance = new HashMap<>();

        for (Grievance grievance : grievances) {
            Department department = grievance.getMappedDepartment();
            eligibleOfficersByGrievance.put(grievance.getId(), userService.findVerifiedOfficersByDepartment(department));
        }

        model.addAttribute("username", authentication.getName());
        model.addAttribute("grievances", grievances);
        model.addAttribute("eligibleOfficersByGrievance", eligibleOfficersByGrievance);
        model.addAttribute("priorities", GrievancePriority.values());
        return "admin-grievance-management";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Authentication authentication, Model model) {
        List<User> users = userService.findAllUsers();
        long pendingOfficerVerification = users.stream()
            .filter(u -> u.getRole() == UserRole.OFFICER && !u.isVerified())
            .count();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("users", users);
        model.addAttribute("pendingOfficerVerification", pendingOfficerVerification);
        return "admin-users";
    }

    @PostMapping("/admin/grievances/{id}/assign")
    public String assignGrievance(@PathVariable Long id, @RequestParam Long officerId) {
        grievanceService.assignToOfficer(id, officerId);
        return "redirect:/admin/grievances/manage";
    }

    @PostMapping("/admin/grievances/{id}/details")
    public String updateGrievanceDetails(
        @PathVariable Long id,
        @RequestParam GrievancePriority priority,
        @RequestParam(required = false) LocalDate dueDate
    ) {
        grievanceService.updateAdminMetadata(id, priority, dueDate);
        return "redirect:/admin/grievances/manage";
    }

    @PostMapping("/admin/users/{id}/verify-officer")
    public String verifyOfficer(@PathVariable Long id) {
        userService.verifyOfficer(id);
        return "redirect:/admin/users";
    }

    private boolean hasRole(Authentication auth, String roleName) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(roleName)) {
                return true;
            }
        }
        return false;
    }
}
