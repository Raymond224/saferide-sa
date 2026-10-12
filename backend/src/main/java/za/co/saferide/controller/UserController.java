package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.User;
import za.co.saferide.repository.UserRepository;
import za.co.saferide.service.PasswordService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User management endpoints (system-admin and admin):
 *   GET    /api/users             → list all users (optional ?role=)
 *   GET    /api/users/:id         → one user
 *   POST   /api/users             → create user
 *   PUT    /api/users/:id/role    → change role
 *   PUT    /api/users/:id/active  → activate / suspend
 *   DELETE /api/users/:id         → soft delete
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserController(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String role,
                                   HttpSession session) {
        Object userId = session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(sessionRole) && !"admin".equals(sessionRole)) {
            return error(HttpStatus.FORBIDDEN, "Not allowed");
        }

        List<User> users = userRepository.findAll(role);
        // Convert to safe public maps (no password hashes)
        List<Map<String, Object>> out = users.stream().map(this::toPublic).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        return ResponseEntity.ok(toPublic(found.get()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserRequest body, HttpSession session) {
        String sessionRole = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(sessionRole) && !"admin".equals(sessionRole)) {
            return error(HttpStatus.FORBIDDEN, "Not allowed");
        }
        if (body == null || body.username == null || body.password == null || body.role == null) {
            return error(HttpStatus.BAD_REQUEST, "username, password and role are required");
        }
        if (userRepository.findByUsername(body.username).isPresent()) {
            return error(HttpStatus.CONFLICT, "Username already exists");
        }
        try {
            User.Role.fromDb(body.role);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid role: " + body.role);
        }

        User u = new User();
        u.setUsername(body.username);
        u.setPasswordHash(passwordService.hash(body.password));
        u.setRole(User.Role.fromDb(body.role));
        u.setFullName(body.fullName == null ? body.username : body.fullName);
        u.setEmail(body.email);
        u.setPhone(body.phone);
        u.setSchoolId(body.schoolId);
        u.setActive(true);

        Long id = userRepository.insert(u);
        return ResponseEntity.ok(Map.of("id", id, "ok", true));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         HttpSession session) {
        String sessionRole = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(sessionRole)) {
            return error(HttpStatus.FORBIDDEN, "Only system admins can change roles");
        }
        String newRole = body.get("role");
        if (newRole == null) {
            return error(HttpStatus.BAD_REQUEST, "role is required");
        }
        try {
            User.Role.fromDb(newRole);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole);
        }
        int updated = userRepository.updateRole(id, newRole);
        if (updated == 0) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id, "role", newRole));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<?> updateActive(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           HttpSession session) {
        String sessionRole = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(sessionRole) && !"admin".equals(sessionRole)) {
            return error(HttpStatus.FORBIDDEN, "Not allowed");
        }
        Object activeObj = body.get("active");
        if (!(activeObj instanceof Boolean)) {
            return error(HttpStatus.BAD_REQUEST, "active must be true or false");
        }
        boolean active = (Boolean) activeObj;
        int updated = userRepository.updateActive(id, active);
        if (updated == 0) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id, "active", active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id, HttpSession session) {
        String sessionRole = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(sessionRole)) {
            return error(HttpStatus.FORBIDDEN, "Only system admins can delete users");
        }
        int updated = userRepository.softDelete(id);
        if (updated == 0) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    // -------- helpers --------

    private Map<String, Object> toPublic(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRoleString());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("schoolId", u.getSchoolId());
        m.put("active", u.isActive());
        m.put("createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        return m;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    public static class CreateUserRequest {
        public String username;
        public String password;
        public String role;
        public String fullName;
        public String email;
        public String phone;
        public Long schoolId;
    }
}
