package com.dreamarchive.auth;

import com.dreamarchive.controller.AdminController;
import com.dreamarchive.controller.DreamController;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.entity.User;
import com.dreamarchive.interceptor.JwtAuthInterceptor;
import com.dreamarchive.mapper.CommentMapper;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.mapper.UserMapper;
import com.dreamarchive.service.DreamService;
import com.dreamarchive.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRegressionTest {

    private MockMvc mockMvc;

    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private DreamMapper dreamMapper;
    private CommentMapper commentMapper;
    private DreamService dreamService;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userMapper = mock(UserMapper.class);
        dreamMapper = mock(DreamMapper.class);
        commentMapper = mock(CommentMapper.class);
        dreamService = mock(DreamService.class);

        AdminController adminController = new AdminController();
        ReflectionTestUtils.setField(adminController, "userMapper", userMapper);
        ReflectionTestUtils.setField(adminController, "dreamMapper", dreamMapper);
        ReflectionTestUtils.setField(adminController, "commentMapper", commentMapper);

        DreamController dreamController = new DreamController();
        ReflectionTestUtils.setField(dreamController, "dreamService", dreamService);
        ReflectionTestUtils.setField(dreamController, "dreamMapper", dreamMapper);

        JwtAuthInterceptor interceptor = new JwtAuthInterceptor(jwtUtil, new ObjectMapper());

        mockMvc = MockMvcBuilders
                .standaloneSetup(adminController, dreamController)
                .addInterceptors(interceptor)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void normalUserShouldGet403WhenAccessingAdminApi() throws Exception {
        configureToken("user-token", 2L, "USER", "u2");

        User user = new User();
        user.setId(2L);
        user.setRole("USER");
        user.setStatus(1);
        when(userMapper.findById(2L)).thenReturn(user);

        mockMvc.perform(get("/admin/stats")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void nonOwnerShouldGet403WhenUpdatingDream() throws Exception {
        configureToken("other-user-token", 10L, "USER", "u10");

        Dream existing = new Dream();
        existing.setId(1L);
        existing.setUserId(9L);
        when(dreamMapper.findById(1L)).thenReturn(existing);

        mockMvc.perform(put("/dream/1")
                        .header("Authorization", "Bearer other-user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void ownerShouldBeAbleToUpdateOwnDream() throws Exception {
        configureToken("owner-token", 9L, "USER", "owner");

        Dream existing = new Dream();
        existing.setId(1L);
        existing.setUserId(9L);
        when(dreamMapper.findById(1L)).thenReturn(existing);
        when(dreamService.updateDream(any(Dream.class))).thenReturn(true);

        mockMvc.perform(put("/dream/1")
                        .header("Authorization", "Bearer owner-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Dream> captor = ArgumentCaptor.forClass(Dream.class);
        verify(dreamService, times(1)).updateDream(captor.capture());
        Dream submitted = captor.getValue();
        Assertions.assertEquals(9L, submitted.getUserId());
        Assertions.assertEquals(1L, submitted.getId());
    }

    private void configureToken(String token, Long userId, String role, String username) {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.getSubject()).thenReturn(username);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
    }
}
