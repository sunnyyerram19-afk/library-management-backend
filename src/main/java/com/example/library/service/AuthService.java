package com.example.library.service;

import com.example.library.dto.AuthResponse;
import com.example.library.dto.LoginRequest;
import com.example.library.dto.RegisterRequest;
import com.example.library.entity.Member;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.exception.ConflictException;
import com.example.library.repository.MemberRepository;
import com.example.library.repository.UserRepository;
import com.example.library.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /** Self registration: creates a MEMBER login plus a member profile. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();

        if (users.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        if (members.existsByEmail(email)) {
            throw new ConflictException("A member with this email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(req.password()));
        user.setRole(Role.MEMBER);
        users.save(user);

        Member member = new Member();
        member.setName(req.name().trim());
        member.setEmail(email);
        member.setPhone(req.phone());
        member.setUser(user);
        members.save(member);

        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException (-> 401) when username/password are wrong.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = users.findByUsername(req.username()).orElseThrow();
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String role = user.getRole().name();
        return new AuthResponse(jwtUtil.generateToken(user.getUsername(), role), user.getUsername(), role);
    }
}
