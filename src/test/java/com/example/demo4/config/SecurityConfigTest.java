package com.example.demo4.config;

import com.example.demo4.entity.User;
import com.example.demo4.service.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthTokenService tokenService;

    @Test
    void protectedApiRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/booking/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void adminApiRejectsOrdinaryUserToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenFor(2L, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void operationsApiRejectsOrdinaryUserToken() throws Exception {
        mockMvc.perform(get("/api/operations/overview")
                        .header("Authorization", "Bearer " + tokenFor(2L, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void adminRolePassesAuthorizationLayer() throws Exception {
        mockMvc.perform(get("/api/dashboard/not-found")
                        .header("Authorization", "Bearer " + tokenFor(1L, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void forgedTokenIsRejectedBeforeController() throws Exception {
        mockMvc.perform(get("/api/booking/list")
                        .header("Authorization", "Bearer forged.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void bookingCreateRequiresIdempotencyHeader() throws Exception {
        mockMvc.perform(post("/api/booking/create")
                        .header("Authorization", "Bearer " + tokenFor(2L, 0))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private String tokenFor(Long id, int role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return tokenService.createToken(user);
    }
}
