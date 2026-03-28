package com.menamiit.smartcityproject.config;

import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.repository.UserRepository;
import com.menamiit.smartcityproject.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedUsers(UserService userService, UserRepository userRepository) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                userService.createUser("admin", "admin@smartcity.local", "admin123", UserRole.ADMIN);
            }
            if (!userRepository.existsByUsername("officer1")) {
                userService.createUser("officer1", "officer1@smartcity.local", "officer123", UserRole.OFFICER, true, Department.GENERAL);
            }
            if (!userRepository.existsByUsername("water_officer")) {
                userService.createUser("water_officer", "water_officer@smartcity.local", "officer123", UserRole.OFFICER, true, Department.WATER_SUPPLY);
            }
            if (!userRepository.existsByUsername("electrical_officer")) {
                userService.createUser("electrical_officer", "electrical_officer@smartcity.local", "officer123", UserRole.OFFICER, true, Department.ELECTRICAL);
            }
            if (!userRepository.existsByUsername("works_officer")) {
                userService.createUser("works_officer", "works_officer@smartcity.local", "officer123", UserRole.OFFICER, true, Department.PUBLIC_WORKS);
            }
            if (!userRepository.existsByUsername("sanitation_officer")) {
                userService.createUser("sanitation_officer", "sanitation_officer@smartcity.local", "officer123", UserRole.OFFICER, true, Department.SANITATION);
            }
            if (!userRepository.existsByUsername("parks_officer")) {
                userService.createUser("parks_officer", "parks_officer@smartcity.local", "officer123", UserRole.OFFICER, true, Department.PARKS);
            }
        };
    }
}
