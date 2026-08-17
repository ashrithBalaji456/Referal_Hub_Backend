package com.referral.outreach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.dto.LoginRequest;
import com.referral.outreach.dto.SignUpRequest;
import com.referral.outreach.entity.PasswordResetToken;
import com.referral.outreach.entity.User;
import com.referral.outreach.repository.PasswordResetTokenRepository;
import com.referral.outreach.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @BeforeEach
    public void setup() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        jakarta.mail.internet.MimeMessage mockMimeMessage = org.mockito.Mockito.mock(jakarta.mail.internet.MimeMessage.class);
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    public void testSignUp_Success() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .username("newuser")
                .email("newuser@gmail.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully!")));

        assertTrue(userRepository.existsByUsername("newuser"));
    }

    @Test
    public void testLogin_Success() throws Exception {
        User user = userRepository.save(User.builder()
                .username("loginuser")
                .email("loginuser@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .build());

        LoginRequest request = LoginRequest.builder()
                .username("loginuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.username", is("loginuser")));
    }

    @Test
    public void testForgotPassword_Success() throws Exception {
        User user = userRepository.save(User.builder()
                .username("resetuser")
                .email("resetuser@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .build());

        Map<String, String> request = new HashMap<>();
        request.put("email", "resetuser@gmail.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If that email exists in our system, we've sent a link to reset your password.")));

        // Verify token created
        assertEquals(1, tokenRepository.count());
    }

    @Test
    public void testResetPassword_Success() throws Exception {
        User user = userRepository.save(User.builder()
                .username("user")
                .email("user@gmail.com")
                .password(passwordEncoder.encode("oldpass"))
                .build());

        PasswordResetToken token = tokenRepository.save(PasswordResetToken.builder()
                .token("valid-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build());

        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-token");
        request.put("newPassword", "newpass123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password has been reset successfully.")));

        // Verify password updated in database
        User updatedUser = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches("newpass123", updatedUser.getPassword()));

        // Verify token deleted
        assertFalse(tokenRepository.findByToken("valid-token").isPresent());
    }
}
