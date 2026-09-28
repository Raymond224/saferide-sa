package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.saferide.repository.ComplianceRepository;

import java.util.Map;

/**
 * Compliance endpoints:
 *   GET /api/compliance          → all operators with status per document
 *   GET /api/compliance/alerts   → only non-compliant / expiring-soon operators
 *   GET /api/compliance/summary  → counts for the dashboard
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceRepository complianceRepository;

    public ComplianceController(ComplianceRepository complianceRepository) {
        this.complianceRepository = complianceRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(complianceRepository.listAll());
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> alerts(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(complianceRepository.listAlerts());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(complianceRepository.summary());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
