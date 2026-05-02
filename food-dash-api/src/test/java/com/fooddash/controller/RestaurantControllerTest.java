package com.fooddash.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RestaurantControllerTest {

	@LocalServerPort
	private int port;

	@Test
	void restaurantOwnerCanCreateAndUpdateOwnRestaurantOnly() throws Exception {
		String owner1Email = "owner1." + UUID.randomUUID() + "@mail.com";
		String owner2Email = "owner2." + UUID.randomUUID() + "@mail.com";
		String password = "OwnerPass123";
		registerUser(owner1Email, password, "Owner One", "+966666666661", "RESTAURANT_OWNER");
		registerUser(owner2Email, password, "Owner Two", "+966666666662", "RESTAURANT_OWNER");

		String owner1Token = login(owner1Email, password);
		String owner2Token = login(owner2Email, password);

		String createBody = """
				{
				  "name":"Spice House",
				  "description":"Indian cuisine",
				  "address":"MG Road, Bengaluru",
				  "latitude":12.9716,
				  "longitude":77.5946,
				  "cuisineType":"Indian",
				  "openingHours":{"monday":"09:00-22:00"},
				  "status":"ACTIVE"
				}
				""";
		HttpResponse<String> createResponse = postJson("/api/v1/restaurants", createBody, owner1Token);
		assertEquals(201, createResponse.statusCode());
		Long restaurantId = extractLongValue(createResponse.body(), "id");
		assertNotNull(restaurantId);

		String updateBody = """
				{
				  "name":"Spice House Updated",
				  "description":"Indian cuisine updated",
				  "address":"Brigade Road, Bengaluru",
				  "latitude":12.9720,
				  "longitude":77.5950,
				  "cuisineType":"Indian",
				  "openingHours":{"monday":"10:00-23:00"},
				  "status":"ACTIVE"
				}
				""";
		HttpResponse<String> ownerUpdateResponse =
				putJson("/api/v1/restaurants/" + restaurantId, updateBody, owner1Token);
		assertEquals(200, ownerUpdateResponse.statusCode());
		assertTrue(ownerUpdateResponse.body().contains("Spice House Updated"));

		HttpResponse<String> otherOwnerUpdateResponse =
				putJson("/api/v1/restaurants/" + restaurantId, updateBody, owner2Token);
		assertEquals(403, otherOwnerUpdateResponse.statusCode());
	}

	@Test
	void publicListingSupportsFiltersAndAdminOnlyDelete() throws Exception {
		String adminEmail = "admin.restaurant." + UUID.randomUUID() + "@mail.com";
		String ownerEmail = "owner.restaurant." + UUID.randomUUID() + "@mail.com";
		String password = "Pass12345";
		registerUser(adminEmail, password, "Admin User", "+977777777771", "ADMIN");
		registerUser(ownerEmail, password, "Owner User", "+977777777772", "RESTAURANT_OWNER");

		String adminToken = login(adminEmail, password);
		String ownerToken = login(ownerEmail, password);

		String nearRestaurant = """
				{
				  "name":"Near Indian",
				  "description":"Near center",
				  "address":"Central Area",
				  "latitude":12.9716,
				  "longitude":77.5946,
				  "cuisineType":"Indian",
				  "openingHours":{"all":"09:00-21:00"},
				  "status":"ACTIVE"
				}
				""";
		HttpResponse<String> nearCreateResponse = postJson("/api/v1/restaurants", nearRestaurant, ownerToken);
		assertEquals(201, nearCreateResponse.statusCode());
		Long nearRestaurantId = extractLongValue(nearCreateResponse.body(), "id");
		assertNotNull(nearRestaurantId);

		String farRestaurant = """
				{
				  "name":"Far Indian",
				  "description":"Far away",
				  "address":"Far Area",
				  "latitude":13.2000,
				  "longitude":77.0000,
				  "cuisineType":"Indian",
				  "openingHours":{"all":"09:00-21:00"},
				  "status":"ACTIVE"
				}
				""";
		assertEquals(201, postJson("/api/v1/restaurants", farRestaurant, ownerToken).statusCode());

		String nearOtherCuisine = """
				{
				  "name":"Near Italian",
				  "description":"Near center but different cuisine",
				  "address":"Central Area",
				  "latitude":12.9720,
				  "longitude":77.5950,
				  "cuisineType":"Italian",
				  "openingHours":{"all":"09:00-21:00"},
				  "status":"ACTIVE"
				}
				""";
		assertEquals(201, postJson("/api/v1/restaurants", nearOtherCuisine, ownerToken).statusCode());

		HttpResponse<String> listResponse = get(
				"/api/v1/restaurants?cuisine=Indian&latitude=12.9716&longitude=77.5946&radiusKm=5&page=0&size=10",
				null);
		assertEquals(200, listResponse.statusCode());
		assertTrue(listResponse.body().contains("Near Indian"));
		assertTrue(!listResponse.body().contains("Far Indian"));
		assertTrue(!listResponse.body().contains("Near Italian"));

		HttpResponse<String> detailsResponse = get("/api/v1/restaurants/" + nearRestaurantId, null);
		assertEquals(200, detailsResponse.statusCode());

		HttpResponse<String> ownerDeleteResponse = delete("/api/v1/restaurants/" + nearRestaurantId, ownerToken);
		assertEquals(403, ownerDeleteResponse.statusCode());

		HttpResponse<String> adminDeleteResponse = delete("/api/v1/restaurants/" + nearRestaurantId, adminToken);
		assertEquals(200, adminDeleteResponse.statusCode());

		HttpResponse<String> deletedDetailsResponse = get("/api/v1/restaurants/" + nearRestaurantId, null);
		assertEquals(404, deletedDetailsResponse.statusCode());
	}

	private void registerUser(String email, String password, String fullName, String phone, String role) throws Exception {
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
	}

	private String login(String email, String password) throws Exception {
		String loginBody = """
				{
				  "email":"%s",
				  "password":"%s"
				}
				""".formatted(email, password);
		HttpResponse<String> response = postJson("/api/v1/auth/login", loginBody, null);
		assertEquals(200, response.statusCode());
		String accessToken = extractStringValue(response.body(), "accessToken");
		assertNotNull(accessToken);
		return accessToken;
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
}
