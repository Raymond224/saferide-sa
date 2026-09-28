package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.repository.SchoolRepository;

import java.util.Map;

/**
 * School endpoints:
 *   GET  /api/schools        → list all schools (with user counts)
 *   GET  /api/schools/:id    → one school
 *   POST /api/schools        → create (system-admin only)
 */
@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolRepository schoolRepository;

    public SchoolController(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(schoolRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return schoolRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "School not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSchoolRequest body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(role) && !"admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only system admins can add schools");
        }
        if (body == null || body.name == null || body.name.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "name is required");
        }
        Long id = schoolRepository.insert(body.name, body.address, body.phone, body.email);
        return ResponseEntity.ok(Map.of("id", id, "ok", true));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    public static class CreateSchoolRequest {
        public String name;
        public String address;
        public String phone;
        public String email;
    }
}
