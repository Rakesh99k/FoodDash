package com.fooddash.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddash.model.User;
import com.fooddash.repository.UserRepository;
import com.fooddash.support.AbstractIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerTest extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerCreatesUserWithHashedPassword() throws Exception {
		String email = "user." + UUID.randomUUID() + "@mail.com";
		String password = "StrongPass123";
		String body = """
				{
				  "email":"%s",
				  "password":"%s",
				  "fullName":"Test User",
				  "phone":"+911234567890"
				}
				""".formatted(email, password);

		HttpResponse<String> registerResponse = postJson("/api/v1/auth/register", body);
		assertEquals(201, registerResponse.statusCode());
		assertTrue(registerResponse.body().contains("\"status\":\"success\""));

		User savedUser = userRepository.findByEmail(email).orElseThrow();
		assertNotEquals(password, savedUser.getPasswordHash());
		assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
	}

	@Test
	void loginReturnsTokensAndRoleAccessIsEnforced() throws Exception {
		String email = "customer." + UUID.randomUUID() + "@mail.com";
		String password = "StrongPass123";
		String registerBody = """
				{
				  "email":"%s",
				  "password":"%s",
				  "fullName":"Customer User",
				  "phone":"+911234560001",
				  "role":"CUSTOMER"
				}
				""".formatted(email, password);
		assertEquals(201, postJson("/api/v1/auth/register", registerBody).statusCode());

		String loginBody = """
				{
				  "email":"%s",
				  "password":"%s"
				}
				""".formatted(email, password);
		HttpResponse<String> loginResponse = postJson("/api/v1/auth/login", loginBody);
		assertEquals(200, loginResponse.statusCode());

		String accessToken = extractJsonValue(loginResponse.body(), "accessToken");
		String refreshToken = extractJsonValue(loginResponse.body(), "refreshToken");
		assertTrue(accessToken != null && !accessToken.isBlank());
		assertTrue(refreshToken != null && !refreshToken.isBlank());

		HttpResponse<String> withoutTokenResponse = get("/api/v1/secure/customer", null);
		assertEquals(401, withoutTokenResponse.statusCode());

		HttpResponse<String> customerEndpointResponse = get("/api/v1/secure/customer", accessToken);
		assertEquals(200, customerEndpointResponse.statusCode());

		HttpResponse<String> adminEndpointResponse = get("/api/v1/secure/admin", accessToken);
		assertEquals(403, adminEndpointResponse.statusCode());
	}

	private HttpResponse<String> postJson(String path, String json) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path, String bearerToken) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest.Builder builder =
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
		if (bearerToken != null) {
			builder.header("Authorization", "Bearer " + bearerToken);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private String extractJsonValue(String json, String key) {
		Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(json);
		return matcher.find() ? matcher.group(1) : null;
	}
}
