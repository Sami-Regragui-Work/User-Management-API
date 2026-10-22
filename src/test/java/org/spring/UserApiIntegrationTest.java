package org.spring;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.config.RootConfig;
import org.spring.config.WebConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserApiIntegrationTest {
    private static AnnotationConfigWebApplicationContext rootContext;
    private static AnnotationConfigWebApplicationContext webContext;
    private static MockMvc mockMvc;

    private PlatformTransactionManager txManager;
    private TransactionStatus tx;

    private static final String ADMIN = basic("admin@example.com", "admin@example.com");
    private static final String USER1 = basic("user1@example.com", "user1@example.com");
    private static final String DEACTIVATED = basic("d_user@example.com", "d_user@example.com");

    private static String basic(String email, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    // ---------- setup: builds the same Root + Web context hierarchy as SpringMvcInitializer ----------

    @BeforeAll
    static void startContexts() {
        MockServletContext servletContext = new MockServletContext();

        rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.setServletContext(servletContext);
        rootContext.register(RootConfig.class);
        rootContext.refresh();

        webContext = new AnnotationConfigWebApplicationContext();
        webContext.setServletContext(servletContext);
        webContext.setParent(rootContext);
        webContext.register(WebConfig.class);
        webContext.refresh();

        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @AfterAll
    static void closeContexts() {
        webContext.close();
        rootContext.close();
    }

    // Each test runs inside a transaction that is rolled back, so tests never affect each other.
    // The seeded users were inserted at startup, outside these transactions, so they survive.
    @BeforeEach
    void beginTransaction() {
        txManager = rootContext.getBean(PlatformTransactionManager.class);
        tx = txManager.getTransaction(new DefaultTransactionDefinition());
    }

    @AfterEach
    void rollbackTransaction() {
        txManager.rollback(tx);
    }

    // ---------- 401 / 403 ----------

    @Test
    void noCredentials_is401_withChallengeHeader() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deactivatedUser_cannotLogIn() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, DEACTIVATED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userOnAdminEndpoint_is403() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, USER1))
                .andExpect(status().isForbidden());
    }

    // ---------- admin: list ----------

    @Test
    void adminList_hidesInactive_andNeverReturnsPasswords() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist());
    }

    @Test
    void adminList_withSize_returnsOnlyThatMany() throws Exception {
        mockMvc.perform(get("/api/users?size=2").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- admin: create ----------

    @Test
    void createUser_invalidBody_is400_withFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"bad","password":"short","role":"USER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void createUser_duplicateEmail_is409() throws Exception {
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","email":"user1@example.com","password":"password123","role":"USER"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_valid_is201_withoutPassword() throws Exception {
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New","email":"new@example.com","password":"password123","role":"USER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ---------- /me ----------

    @Test
    void me_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, USER1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    void changePassword_wrongCurrent_is400() throws Exception {
        mockMvc.perform(patch("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong","newPassword":"newPassword1"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_ok_is204() throws Exception {
        mockMvc.perform(patch("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, USER1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"user1@example.com","newPassword":"newPassword1"}"""))
                .andExpect(status().isNoContent());
    }

    // ---------- 404 and deactivation ----------

    @Test
    void unknownId_is404() throws Exception {
        mockMvc.perform(get("/api/users/999999").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivate_then_get_is404() throws Exception {
        String body = mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, USER1))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id").toString();

        mockMvc.perform(delete("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNotFound());
    }
}