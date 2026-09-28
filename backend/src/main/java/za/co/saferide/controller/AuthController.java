package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.User;
import za.co.saferide.repository.UserRepository;
import za.co.saferide.service.PasswordService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication endpoints:
 *   POST /api/auth/login   { username, password }  → { id, username, role, ... }
 *   POST /api/auth/logout
 *   GET  /api/auth/me                              → current session user
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AuthController(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpSession session) {
        if (body == null || body.username == null || body.password == null) {
            return error(HttpStatus.BAD_REQUEST, "username and password are required");
        }

        Optional<User> found = userRepository.findByUsername(body.username);
        if (found.isEmpty()) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        User user = found.get();
        if (!passwordService.verify(body.password, user.getPasswordHash())) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRoleString());
        session.setAttribute("schoolId", user.getSchoolId());

        return ResponseEntity.ok(toPublic(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<User> found = userRepository.findById(((Number) userId).longValue());
        if (found.isEmpty()) {
            session.invalidate();
            return error(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return ResponseEntity.ok(toPublic(found.get()));
    }

    // -------- helpers --------

    private Map<String, Object> toPublic(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRoleString());
        m.put("schoolId", u.getSchoolId());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        return m;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /** Request body for login. */
    public static class LoginRequest {
        public String username;
        public String password;
    }
}
