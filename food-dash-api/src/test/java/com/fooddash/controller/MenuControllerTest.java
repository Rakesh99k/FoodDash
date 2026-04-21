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
class MenuControllerTest {

	@LocalServerPort
	private int port;

	@Test
	void publicCanViewRestaurantMenuWithFilters() throws Exception {
		String ownerEmail = "menu.owner." + UUID.randomUUID() + "@mail.com";
		String password = "OwnerPass123";
		registerUser(ownerEmail, password, "Menu Owner", uniquePhone(), "RESTAURANT_OWNER");
		String ownerToken = login(ownerEmail, password);

		Long restaurantId = createRestaurant(ownerToken, "Menu Hub");
		assertNotNull(restaurantId);

		String availableItem = """
				{
				  "name":"Paneer Tikka",
				  "description":"Starter",
				  "price":220.00,
				  "category":"Starter",
				  "imageUrl":"https://example.com/paneer.jpg",
				  "available":true
				}
				""";
		String unavailableItem = """
				{
				  "name":"Chocolate Cake",
				  "description":"Dessert",
				  "price":180.00,
				  "category":"Dessert",
				  "imageUrl":"https://example.com/cake.jpg",
				  "available":false
				}
				""";
		assertEquals(201, postJson("/api/v1/restaurants/" + restaurantId + "/menu", availableItem, ownerToken).statusCode());
		assertEquals(201, postJson("/api/v1/restaurants/" + restaurantId + "/menu", unavailableItem, ownerToken).statusCode());

		HttpResponse<String> publicListResponse = get("/api/v1/restaurants/" + restaurantId + "/menu", null);
		assertEquals(200, publicListResponse.statusCode());
		assertTrue(publicListResponse.body().contains("Paneer Tikka"));
		assertTrue(publicListResponse.body().contains("Chocolate Cake"));

		HttpResponse<String> filteredResponse =
				get("/api/v1/restaurants/" + restaurantId + "/menu?category=Starter&available=true", null);
		assertEquals(200, filteredResponse.statusCode());
		assertTrue(filteredResponse.body().contains("Paneer Tikka"));
		assertTrue(!filteredResponse.body().contains("Chocolate Cake"));
	}

	@Test
	void ownersAndAdminsCanManageMenuItemsWithOwnershipChecks() throws Exception {
		String owner1Email = "owner1.menu." + UUID.randomUUID() + "@mail.com";
		String owner2Email = "owner2.menu." + UUID.randomUUID() + "@mail.com";
		String adminEmail = "admin.menu." + UUID.randomUUID() + "@mail.com";
		String password = "Pass12345";

		registerUser(owner1Email, password, "Owner One", uniquePhone(), "RESTAURANT_OWNER");
		registerUser(owner2Email, password, "Owner Two", uniquePhone(), "RESTAURANT_OWNER");
		registerUser(adminEmail, password, "Admin Menu", uniquePhone(), "ADMIN");

		String owner1Token = login(owner1Email, password);
		String owner2Token = login(owner2Email, password);
		String adminToken = login(adminEmail, password);

		Long restaurantId = createRestaurant(owner1Token, "Owner One Restaurant");
		assertNotNull(restaurantId);

		String createItemBody = """
				{
				  "name":"Veg Burger",
				  "description":"Classic burger",
				  "price":150.00,
				  "category":"Main",
				  "imageUrl":"https://example.com/burger.jpg",
				  "available":true
				}
				""";
		HttpResponse<String> createItemResponse =
				postJson("/api/v1/restaurants/" + restaurantId + "/menu", createItemBody, owner1Token);
		assertEquals(201, createItemResponse.statusCode());
		Long itemId = extractLongValue(createItemResponse.body(), "id");
		assertNotNull(itemId);

		HttpResponse<String> nonOwnerCreateResponse =
				postJson("/api/v1/restaurants/999999/menu", createItemBody, owner1Token);
		assertEquals(404, nonOwnerCreateResponse.statusCode());

		String updateBody = """
				{
				  "name":"Veg Burger XL",
				  "description":"Updated burger",
				  "price":175.00,
				  "category":"Main",
				  "imageUrl":"https://example.com/burger-xl.jpg",
				  "available":true
				}
				""";
		HttpResponse<String> unauthorizedUpdate = putJson("/api/v1/menu/" + itemId, updateBody, owner2Token);
		assertEquals(403, unauthorizedUpdate.statusCode());

		HttpResponse<String> adminUpdate = putJson("/api/v1/menu/" + itemId, updateBody, adminToken);
		assertEquals(200, adminUpdate.statusCode());
		assertTrue(adminUpdate.body().contains("Veg Burger XL"));

		HttpResponse<String> ownerDelete = delete("/api/v1/menu/" + itemId, owner1Token);
		assertEquals(200, ownerDelete.statusCode());
		assertTrue(ownerDelete.body().contains("\"available\":false"));
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

	private Long createRestaurant(String token, String name) throws Exception {
		String body = """
				{
				  "name":"%s",
				  "description":"Test restaurant",
				  "address":"Sample address",
				  "latitude":12.9716,
				  "longitude":77.5946,
				  "cuisineType":"Indian",
				  "openingHours":{"all":"09:00-22:00"},
				  "status":"ACTIVE"
				}
				""".formatted(name);
		HttpResponse<String> response = postJson("/api/v1/restaurants", body, token);
		assertEquals(201, response.statusCode());
		return extractLongValue(response.body(), "id");
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

	private String uniquePhone() {
		String digits = String.valueOf(Math.abs(System.nanoTime()));
		String padded = (digits + "0000000000").substring(0, 10);
		return "+9" + padded;
	}
}
