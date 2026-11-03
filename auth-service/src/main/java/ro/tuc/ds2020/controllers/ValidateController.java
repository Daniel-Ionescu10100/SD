package ro.tuc.ds2020.controllers;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.tuc.ds2020.services.JwtService;

@RestController
@RequestMapping("/auth")
public class ValidateController {

    private final JwtService jwtService;

    @Autowired
    public ValidateController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.validateAndExtract(token);
            return ResponseEntity.ok()
                    .header("X-User-Id",  claims.get("userId", String.class))
                    .header("X-Username", claims.getSubject())
                    .header("X-Role",     claims.get("role", String.class))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
