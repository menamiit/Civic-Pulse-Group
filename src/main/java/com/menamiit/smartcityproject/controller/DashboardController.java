package com.menamiit.smartcityproject.controller;

import com.menamiit.smartcityproject.entity.GrievanceStatus;
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

    @GetMapping("/citizen/home")
    public String citizenHome(Authentication authentication, Model model) {
        model.addAttribute("grievances", grievanceService.getCitizenGrievances(authentication.getName()));
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
        model.addAttribute("grievances", grievanceService.getOfficerGrievances(authentication.getName()));
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
    public String adminHome(Model model) {
        model.addAttribute("grievances", grievanceService.getAllGrievances());
        model.addAttribute("officers", userService.findAllByRole(UserRole.OFFICER));
        model.addAttribute("users", userService.findAllUsers());
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
