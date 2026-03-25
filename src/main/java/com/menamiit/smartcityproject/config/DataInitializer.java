package com.menamiit.smartcityproject.config;

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
                userService.createUser("officer1", "officer1@smartcity.local", "officer123", UserRole.OFFICER);
            }
        };
    }
}
