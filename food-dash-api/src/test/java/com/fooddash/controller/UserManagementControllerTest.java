package com.fooddash.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddash.support.AbstractIntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class UserManagementControllerTest extends AbstractIntegrationTest {

	@Test
	void userCanViewUpdateProfileAndChangePassword() throws Exception {
		String email = "profile." + UUID.randomUUID() + "@mail.com";
		String oldPassword = "StrongPass123";
		String newPassword = "StrongPass456";
		String phone = "+911111111111";
		registerUser(email, oldPassword, "Profile User", phone, "CUSTOMER");

		String accessToken = login(email, oldPassword).accessToken();
		HttpResponse<String> meResponse = get("/api/v1/users/me", accessToken);
		assertEquals(200, meResponse.statusCode());
		assertTrue(meResponse.body().contains(email));

		String updateBody = """
				{
				  "fullName":"Updated Profile User",
				  "phone":"+922222222222"
				}
				""";
		HttpResponse<String> updateResponse = putJson("/api/v1/users/me", updateBody, accessToken);
		assertEquals(200, updateResponse.statusCode());
		assertTrue(updateResponse.body().contains("Updated Profile User"));

		String changePasswordBody = """
				{
				  "oldPassword":"%s",
				  "newPassword":"%s"
				}
				""".formatted(oldPassword, newPassword);
		HttpResponse<String> passwordChangeResponse =
				postJson("/api/v1/users/me/change-password", changePasswordBody, accessToken);
		assertEquals(200, passwordChangeResponse.statusCode());

		HttpResponse<String> oldLoginResponse = loginRaw(email, oldPassword);
		assertEquals(401, oldLoginResponse.statusCode());

		HttpResponse<String> newLoginResponse = loginRaw(email, newPassword);
		assertEquals(200, newLoginResponse.statusCode());
	}

	@Test
	void adminCanManageUsersAndDeactivatedUserCannotLogin() throws Exception {
		String adminEmail = "admin." + UUID.randomUUID() + "@mail.com";
		String adminPassword = "AdminPass123";
		registerUser(adminEmail, adminPassword, "Admin User", "+933333333333", "ADMIN");

		String targetEmail = "target." + UUID.randomUUID() + "@mail.com";
		String targetPassword = "TargetPass123";
		String registerResponse =
				registerUser(targetEmail, targetPassword, "Target User", "+944444444444", "CUSTOMER");
		Long targetUserId = extractLongValue(registerResponse, "id");
		assertNotNull(targetUserId);

		String adminToken = login(adminEmail, adminPassword).accessToken();
		String customerToken = login(targetEmail, targetPassword).accessToken();

		HttpResponse<String> forbiddenResponse = get("/api/v1/admin/users", customerToken);
		assertEquals(403, forbiddenResponse.statusCode());

		HttpResponse<String> listResponse = get("/api/v1/admin/users?page=0&size=10&role=CUSTOMER", adminToken);
		assertEquals(200, listResponse.statusCode());
		assertTrue(listResponse.body().contains(targetEmail));

		HttpResponse<String> getByIdResponse = get("/api/v1/admin/users/" + targetUserId, adminToken);
		assertEquals(200, getByIdResponse.statusCode());

		String updateBody = """
				{
				  "email":"%s",
				  "fullName":"Updated Target User",
				  "phone":"+955555555555",
				  "role":"DELIVERY_PERSON",
				  "active":true
				}
				""".formatted(targetEmail);
		HttpResponse<String> updateResponse = putJson("/api/v1/admin/users/" + targetUserId, updateBody, adminToken);
		assertEquals(200, updateResponse.statusCode());
		assertTrue(updateResponse.body().contains("DELIVERY_PERSON"));

		HttpResponse<String> deactivateResponse = delete("/api/v1/admin/users/" + targetUserId, adminToken);
		assertEquals(200, deactivateResponse.statusCode());
		assertTrue(deactivateResponse.body().contains("\"active\":false"));

		HttpResponse<String> deactivatedLoginResponse = loginRaw(targetEmail, targetPassword);
		assertEquals(401, deactivatedLoginResponse.statusCode());
	}

	private String registerUser(String email, String password, String fullName, String phone, String role) throws Exception {
		String registerBody = """
				{
				  "email":"%s",
				  "password":"%s",
				  "fullName":"%s",
				  "phone":"%s",
				  "role":"%s"
				}
				""".formatted(email, password, fullName, phone, role);
		HttpResponse<String> response = postJson("/api/v1/auth/register", registerBody, null);
		assertEquals(201, response.statusCode());
		return response.body();
	}

	private LoginTokens login(String email, String password) throws Exception {
		HttpResponse<String> response = loginRaw(email, password);
		assertEquals(200, response.statusCode());
		String accessToken = extractStringValue(response.body(), "accessToken");
		String refreshToken = extractStringValue(response.body(), "refreshToken");
		assertNotNull(accessToken);
		assertNotNull(refreshToken);
		return new LoginTokens(accessToken, refreshToken);
	}

	private HttpResponse<String> loginRaw(String email, String password) throws Exception {
		String loginBody = """
				{
				  "email":"%s",
				  "password":"%s"
				}
				""".formatted(email, password);
		return postJson("/api/v1/auth/login", loginBody, null);
	}

	private HttpResponse<String> postJson(String path, String json, String accessToken) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json");
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(json)).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> putJson(String path, String json, String accessToken) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + accessToken)
				.PUT(HttpRequest.BodyPublishers.ofString(json))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path, String accessToken) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest.Builder builder =
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> delete(String path, String accessToken) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Authorization", "Bearer " + accessToken)
				.DELETE()
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String extractStringValue(String json, String key) {
		Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(json);
		return matcher.find() ? matcher.group(1) : null;
	}

	private Long extractLongValue(String json, String key) {
		Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
		Matcher matcher = pattern.matcher(json);
		return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
	}

	private record LoginTokens(String accessToken, String refreshToken) {
	}
}
