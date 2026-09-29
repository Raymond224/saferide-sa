package za.co.saferide.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Password hashing and verification using BCrypt.
 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Hash a plaintext password for storage. */
    public String hash(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    /** Check a plaintext password against a stored hash. */
    public boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        return encoder.matches(plainPassword, storedHash);
    }
}
