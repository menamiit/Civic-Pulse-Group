package com.menamiit.smartcityproject.controller;

import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("departments", Department.values());
        return "register";
    }
    
    @PostMapping("/api/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam UserRole accountType,
            @RequestParam(required = false) Department department,
            Model model) {
        
        try {
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("departments", Department.values());
                return "register";
            }
            
            if (password.length() < 6) {
                model.addAttribute("error", "Password must be at least 6 characters");
                model.addAttribute("departments", Department.values());
                return "register";
            }

            if (accountType == UserRole.ADMIN) {
                model.addAttribute("error", "Admin account cannot be registered from this page");
                model.addAttribute("departments", Department.values());
                return "register";
            }
            
            userService.registerUser(username, email, password, accountType, department);
            model.addAttribute("success", "Registration successful! Please login.");
            if (accountType == UserRole.OFFICER) {
                return "redirect:/login?pending=true";
            }
            return "redirect:/login?success=true";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", Department.values());
            return "register";
        }
    }
}
