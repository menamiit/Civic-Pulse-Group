package com.menamiit.smartcityproject.controller;

import com.menamiit.smartcityproject.entity.GrievanceStatus;
import com.menamiit.smartcityproject.entity.Grievance;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.service.GrievanceService;
import com.menamiit.smartcityproject.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
public class DashboardController {

    private final GrievanceService grievanceService;
    private final UserService userService;

    public DashboardController(GrievanceService grievanceService, UserService userService) {
        this.grievanceService = grievanceService;
        this.userService = userService;
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
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .count();

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole());
        model.addAttribute("verified", user.isVerified());
        model.addAttribute("totalCount", roleGrievances.size());
        model.addAttribute("solvedCount", solvedCount);
        return "profile";
    }

    @GetMapping("/citizen/home")
    public String citizenHome(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances =
            grievanceService.getCitizenGrievances(authentication.getName());

        long solvedCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .count();
        long openCount = grievances.size() - solvedCount;

        List<com.menamiit.smartcityproject.entity.Grievance> latestSubmitted = grievances.stream()
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getCreatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
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
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .count();
        long activeCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.ASSIGNED || g.getStatus() == GrievanceStatus.IN_PROGRESS)
            .count();

        List<com.menamiit.smartcityproject.entity.Grievance> latestAssigned = grievances.stream()
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getUpdatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getUpdatedAt).reversed())
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

    @PostMapping("/officer/grievances/{id}/status")
    public String updateStatus(
        Authentication authentication,
        @PathVariable Long id,
        @RequestParam GrievanceStatus status
    ) {
        grievanceService.updateStatus(id, status, authentication.getName());
        return "redirect:/officer/home";
    }

    @GetMapping("/admin/home")
    public String adminHome(Authentication authentication, Model model) {
        List<com.menamiit.smartcityproject.entity.Grievance> grievances = grievanceService.getAllGrievances();
        List<User> officers = userService.findAllByRole(UserRole.OFFICER);
        List<User> users = userService.findAllUsers();

        long resolvedCount = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .count();
        long unassignedCount = grievances.stream()
            .filter(g -> g.getAssignedOfficer() == null)
            .count();
        long pendingOfficerVerification = users.stream()
            .filter(u -> u.getRole() == UserRole.OFFICER && !u.isVerified())
            .count();

        List<com.menamiit.smartcityproject.entity.Grievance> latestSubmitted = grievances.stream()
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getCreatedAt).reversed())
            .limit(5)
            .toList();

        List<com.menamiit.smartcityproject.entity.Grievance> latestResolved = grievances.stream()
            .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED || g.getStatus() == GrievanceStatus.CLOSED)
            .sorted(Comparator.comparing(com.menamiit.smartcityproject.entity.Grievance::getUpdatedAt).reversed())
            .limit(5)
            .toList();

        model.addAttribute("username", authentication.getName());
        model.addAttribute("grievances", grievances);
        model.addAttribute("officers", officers);
        model.addAttribute("users", users);
        model.addAttribute("totalGrievances", grievances.size());
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("unassignedCount", unassignedCount);
        model.addAttribute("pendingOfficerVerification", pendingOfficerVerification);
        model.addAttribute("latestSubmitted", latestSubmitted);
        model.addAttribute("latestResolved", latestResolved);
        return "admin-home";
    }

    @PostMapping("/admin/grievances/{id}/assign")
    public String assignGrievance(@PathVariable Long id, @RequestParam Long officerId) {
        grievanceService.assignToOfficer(id, officerId);
        return "redirect:/admin/home";
    }

    @PostMapping("/admin/users/{id}/verify-officer")
    public String verifyOfficer(@PathVariable Long id) {
        userService.verifyOfficer(id);
        return "redirect:/admin/home";
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
