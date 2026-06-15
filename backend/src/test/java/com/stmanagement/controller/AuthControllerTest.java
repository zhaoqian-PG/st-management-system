package com.stmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stmanagement.model.User;
import com.stmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserRepository userRepo;
    @Autowired private ObjectMapper objectMapper;

    @Test void testLoginSuccess() throws Exception {
        User user = new User();
        user.setId(1L); user.setUsername("admin"); user.setPassword("admin123"); user.setRole("ADMIN");
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user));

        Map<String,String> body = new HashMap<>();
        body.put("username","admin"); body.put("password","admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test void testLoginFail_wrongPassword() throws Exception {
        User user = new User();
        user.setId(1L); user.setUsername("admin"); user.setPassword("admin123"); user.setRole("ADMIN");
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user));

        Map<String,String> body = new HashMap<>();
        body.put("username","admin"); body.put("password","wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test void testLoginFail_userNotFound() throws Exception {
        when(userRepo.findByUsername("nobody")).thenReturn(Optional.empty());

        Map<String,String> body = new HashMap<>();
        body.put("username","nobody"); body.put("password","any");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
