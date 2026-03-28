package com.menamiit.smartcityproject.service;

import com.menamiit.smartcityproject.entity.Department;
import com.menamiit.smartcityproject.entity.User;
import com.menamiit.smartcityproject.entity.UserRole;
import com.menamiit.smartcityproject.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User registerUser(String username, String email, String password) throws Exception {
        return registerUser(username, email, password, UserRole.CITIZEN, null);
    }

    public User registerUser(String username, String email, String password, UserRole accountType) throws Exception {
        return registerUser(username, email, password, accountType, null);
    }

    public User registerUser(String username, String email, String password, UserRole accountType, Department department) throws Exception {
        if (userRepository.existsByUsername(username)) {
            throw new Exception("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email already registered");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(accountType);
        user.setVerified(accountType != UserRole.OFFICER);
        user.setDepartment(resolveDepartment(accountType, department));
        
        return userRepository.save(user);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean authenticateUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return passwordEncoder.matches(password, user.get().getPassword());
        }
        return false;
    }

    public User createUser(String username, String email, String password, UserRole role) throws Exception {
        return createUser(username, email, password, role, true, null);
    }

    public User createUser(String username, String email, String password, UserRole role, boolean verified) throws Exception {
        return createUser(username, email, password, role, verified, null);
    }

    public User createUser(String username, String email, String password, UserRole role, boolean verified, Department department) throws Exception {
        if (userRepository.existsByUsername(username)) {
            throw new Exception("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setVerified(verified);
        user.setDepartment(resolveDepartment(role, department));

        return userRepository.save(user);
    }

    public List<User> findAllByRole(UserRole role) {
        return userRepository.findAllByRole(role);
    }

    public List<User> findVerifiedOfficersByDepartment(Department department) {
        return userRepository.findAllByRoleAndVerifiedTrueAndDepartment(UserRole.OFFICER, department);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public void verifyOfficer(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != UserRole.OFFICER) {
            throw new IllegalArgumentException("Only officer accounts can be verified");
        }
        user.setVerified(true);
        userRepository.save(user);
    }

    private Department resolveDepartment(UserRole role, Department department) {
        if (role == UserRole.OFFICER) {
            if (department == null) {
                throw new IllegalArgumentException("Officer department is required");
            }
            return department;
        }
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        boolean enabled = !(user.getRole() == UserRole.OFFICER && !user.isVerified());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            enabled,
            true,
            true,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
