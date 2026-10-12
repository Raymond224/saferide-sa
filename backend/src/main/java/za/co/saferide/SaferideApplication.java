package za.co.saferide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaferideApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaferideApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  SafeRide SA backend is running");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================\n");
    }
}
