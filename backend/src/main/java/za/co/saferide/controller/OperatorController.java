package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.Operator;
import za.co.saferide.model.User;
import za.co.saferide.repository.OperatorRepository;
import za.co.saferide.repository.UserRepository;
import za.co.saferide.service.PasswordService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operator endpoints:
 *   GET    /api/operators            → list all active operators (admin, filtered by school)
 *   GET    /api/operators/:id        → single operator
 *   GET    /api/operators/me         → operator row for current logged-in operator user
 *   POST   /api/operators            → create new operator + user account (admin only)
 *   PUT    /api/operators/:id/expiry → update one expiry date (admin only)
 */
@RestController
@RequestMapping("/api/operators")
public class OperatorController {

    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public OperatorController(OperatorRepository operatorRepository,
                              UserRepository userRepository,
                              PasswordService passwordService) {
        this.operatorRepository = operatorRepository;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Long schoolId = sessionSchoolId(session);
        List<Operator> operators;
        if ("system-admin".equals(role)) {
            operators = operatorRepository.findAll(null);
        } else {
            operators = operatorRepository.findAll(schoolId);
        }
        return ResponseEntity.ok(operators);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<Operator> found = operatorRepository.findByUserId(((Number) userId).longValue());
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "No operator profile for this user");
        }
        return ResponseEntity.ok(found.get());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<Operator> found = operatorRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Operator not found");
        }
        return ResponseEntity.ok(found.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateOperatorRequest body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only admins can add operators");
        }
        if (body == null || body.companyName == null || body.companyName.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "companyName is required");
        }
        if (body.username == null || body.password == null) {
            return error(HttpStatus.BAD_REQUEST, "username and password are required");
        }

        Long schoolId = body.schoolId != null ? body.schoolId : sessionSchoolId(session);
        if (schoolId == null) {
            return error(HttpStatus.BAD_REQUEST, "schoolId is required");
        }

        // Check if username already exists
        if (userRepository.findByUsername(body.username).isPresent()) {
            return error(HttpStatus.CONFLICT, "Username already exists");
        }

        // Create the user account first
        User user = new User();
        user.setUsername(body.username);
        user.setPasswordHash(passwordService.hash(body.password));
        user.setRole(User.Role.OPERATOR);
        user.setFullName(body.contactName == null ? body.companyName : body.contactName);
        user.setEmail(body.email);
        user.setPhone(body.phone);
        user.setSchoolId(schoolId);
        user.setActive(true);

        Long newUserId = userRepository.insert(user);

        // Then create the operator row
        Operator op = new Operator();
        op.setUserId(newUserId);
        op.setCompanyName(body.companyName);
        op.setSchoolId(schoolId);
        op.setPrdpExpiry(parseDate(body.prdpExpiry));
        op.setRoadworthyExpiry(parseDate(body.roadworthyExpiry));
        op.setRegistrationExpiry(parseDate(body.registrationExpiry));

        Long opId = operatorRepository.insert(op);

        return ResponseEntity.ok(Map.of(
                "id", opId,
                "userId", newUserId,
                "ok", true
        ));
    }

    @PutMapping("/{id}/expiry")
    public ResponseEntity<?> updateExpiry(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only admins can update compliance dates");
        }
        String field = body.get("field");
        String dateStr = body.get("date");
        if (field == null || dateStr == null) {
            return error(HttpStatus.BAD_REQUEST, "field and date are required");
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            int updated = operatorRepository.updateExpiry(id, field, date);
            if (updated == 0) {
                return error(HttpStatus.NOT_FOUND, "Operator not found");
            }
            return ResponseEntity.ok(Map.of("ok", true, "id", id, "field", field, "date", dateStr));
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid date or field: " + e.getMessage());
        }
    }

    // -------- helpers --------

    private Long sessionSchoolId(HttpSession session) {
        Object v = session.getAttribute("schoolId");
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    public static class CreateOperatorRequest {
        public String username;
        public String password;
        public String companyName;
        public String contactName;
        public String email;
        public String phone;
        public Long schoolId;
        public String prdpExpiry;
        public String roadworthyExpiry;
        public String registrationExpiry;
    }
}
