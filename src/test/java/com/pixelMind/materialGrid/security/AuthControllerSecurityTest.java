package com.pixelMind.materialGrid.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelMind.materialGrid.config.JwtProperties;
import com.pixelMind.materialGrid.config.SecurityConfig;
import com.pixelMind.materialGrid.controller.AuthController;
import com.pixelMind.materialGrid.controller.UserController;
import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.enums.Role;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import com.pixelMind.materialGrid.security.jwt.JwtAccessDeniedHandler;
import com.pixelMind.materialGrid.security.jwt.JwtAuthenticationEntryPoint;
import com.pixelMind.materialGrid.security.jwt.JwtAuthenticationFilter;
import com.pixelMind.materialGrid.security.jwt.JwtTokenProvider;
import com.pixelMind.materialGrid.service.AuthService;
import com.pixelMind.materialGrid.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtTokenProvider.class,
        JwtProperties.class
})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        validJwtToken = jwtTokenProvider.generateToken("admin", 1L, Role.ROLE_ADMIN);

        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("encoded_pwd")
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_ADMIN)
                .build();
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(new SecurityUserDetails(user));
    }

    @Test
    void login_publiclyAccessible_returnsToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("mock-token")
                .sessionToken("mock-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .username("admin")
                .role("ROLE_ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest("admin", "Passw0rd1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-token"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withValidToken_allowsAccess() throws Exception {
        when(userService.getUsers(any())).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
