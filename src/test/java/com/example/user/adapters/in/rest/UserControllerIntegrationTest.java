package com.example.user.adapters.in.rest;

import com.example.user.adapters.in.rest.dto.CreateUserRequest;
import com.example.user.adapters.in.rest.dto.UpdateUserRequest;
import com.example.user.adapters.in.rest.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UserControllerIntegrationTest {

    private static final String TEST_USERNAME = "integration-user";
    private static final String TEST_PASSWORD = "integration-password";
    private static final String TEST_JWT_SECRET = "integration-test-secret-32-chars!!";

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.auth.username", () -> TEST_USERNAME);
        registry.add("app.auth.password-hash",
                () -> new BCryptPasswordEncoder().encode(TEST_PASSWORD));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.getDb().getCollection("users").drop();
    }

    @Test
    @WithMockUser
    void createAndGetUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Alice", "Berlin", 30);

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.address", is("Berlin")))
                .andExpect(jsonPath("$.age", is(30)))
                .andReturn();

        UserResponse created = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);

        mockMvc.perform(get("/api/users/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice")));
    }

    @Test
    @WithMockUser
    void updateUser() throws Exception {
        String id = createUser("Bob", "Hamburg", 25);

        UpdateUserRequest update = new UpdateUserRequest("Bob Updated", "Munich", 26);
        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Bob Updated")))
                .andExpect(jsonPath("$.age", is(26)));
    }

    @Test
    @WithMockUser
    void deleteUser() throws Exception {
        String id = createUser("Charlie", "Frankfurt", 35);

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listUsersWithFilters() throws Exception {
        createUser("Alice", "Berlin", 30);
        createUser("Bob", "Hamburg", 25);
        createUser("Alice2", "Munich", 40);

        // filter by name
        mockMvc.perform(get("/api/users").param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // filter by age
        mockMvc.perform(get("/api/users").param("minAge", "30").param("maxAge", "35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // pagination
        mockMvc.perform(get("/api/users").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    @WithMockUser
    void listUsers_withOnlyMinAge_shouldFilterCorrectly() throws Exception {
        createUser("Young", "Berlin", 20);
        createUser("Old", "Hamburg", 60);

        mockMvc.perform(get("/api/users").param("minAge", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Old")));
    }

    @Test
    @WithMockUser
    void listUsers_withRegexMetaCharactersInName_shouldTreatNameAsLiteral() throws Exception {
        createUser("literal .* pattern", "Berlin", 20);
        createUser("ordinary-user", "Hamburg", 21);

        mockMvc.perform(get("/api/users").param("name", ".*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("literal .* pattern")));
    }

    @Test
    @WithMockUser
    void listUsers_withInvalidAgeRange_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/users").param("minAge", "50").param("maxAge", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request Parameter")))
                .andExpect(jsonPath("$.detail", is("minAge must be less than or equal to maxAge")));
    }

    @Test
    @WithMockUser
    void validationError() throws Exception {
        String badBody = "{\"name\":\"\",\"address\":\"Berlin\",\"age\":30}";
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void login_shouldReturnBearerTokenForValidCredentials() throws Exception {
        String loginBody = """
                {"username":"%s","password":"%s"}
                """.formatted(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds", is(86400)));
    }

    @Test
    void login_shouldRejectInvalidCredentials() throws Exception {
        String loginBody = """
                {"username":"%s","password":"wrong-password"}
                """.formatted(TEST_USERNAME);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String createUser(String name, String address, int age) throws Exception {
        CreateUserRequest req = new CreateUserRequest(name, address, age);
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class).id();
    }
}
