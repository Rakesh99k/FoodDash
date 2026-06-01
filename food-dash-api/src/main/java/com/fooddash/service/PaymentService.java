package com.fooddash.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddash.dto.PaymentInitiateRequest;
import com.fooddash.dto.PaymentResponse;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.FoodOrder;
import com.fooddash.model.OrderStatus;
import com.fooddash.model.Payment;
import com.fooddash.model.PaymentProvider;
import com.fooddash.model.PaymentStatus;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.FoodOrderRepository;
import com.fooddash.repository.PaymentRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final FoodOrderRepository foodOrderRepository;
	private final PaymentRepository paymentRepository;
	private final AuthenticatedUserService authenticatedUserService;
	private final OrderStateMachineService orderStateMachineService;
	private final NotificationService notificationService;

	@Value("${payments.stripe.secret-key:}")
	private String stripeSecretKey;

	@Value("${payments.stripe.webhook-secret:}")
	private String stripeWebhookSecret;

	@Value("${payments.razorpay.key-id:}")
	private String razorpayKeyId;

	@Value("${payments.razorpay.key-secret:}")
	private String razorpayKeySecret;

	@Value("${payments.razorpay.webhook-secret:}")
	private String razorpayWebhookSecret;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Transactional
	public PaymentResponse initiatePayment(Long orderId, PaymentInitiateRequest request) {
		User actor = authenticatedUserService.getCurrentUser();
		FoodOrder order = findOrder(orderId);
		assertCanInitiatePayment(actor, order);

		String currency = normalizeCurrency(request.getCurrency());
		Payment payment = Payment.builder()
				.order(order)
				.amount(order.getTotalAmount())
				.currency(currency)
				.method(request.getMethod())
				.provider(request.getProvider())
				.status(PaymentStatus.PENDING)
				.providerReference("pay_init_" + UUID.randomUUID())
				.build();
		payment = paymentRepository.save(payment);

		ProviderPaymentInit providerInit = switch (request.getProvider()) {
			case STRIPE -> createStripePaymentIntent(payment);
			case RAZORPAY -> createRazorpayOrder(payment);
		};

		payment.setProviderOrderId(providerInit.providerOrderId());
		payment.setProviderPaymentId(providerInit.providerPaymentId());
		payment.setProviderClientSecret(providerInit.clientSecret());
		payment.setStatus(providerInit.status());
		Payment saved = paymentRepository.save(payment);
		return toResponse(saved);
	}

	@Transactional
	public void handleStripeWebhook(String stripeSignatureHeader, String payload) {
		if (!verifyStripeSignature(stripeSignatureHeader, payload)) {
			throw new AuthorizationDeniedException("Invalid Stripe webhook signature");
		}

		JsonNode root = parsePayload(payload);
		String eventId = root.path("id").asText(null);
		if (eventId != null && paymentRepository.existsByWebhookEventId(eventId)) {
			return;
		}

		String eventType = root.path("type").asText("");
		JsonNode objectNode = root.path("data").path("object");
		String providerPaymentId = objectNode.path("id").asText(null);

		Payment payment = resolveStripePayment(root, objectNode, providerPaymentId);
		if (payment == null) {
			log.warn("Stripe webhook ignored because payment could not be mapped");
			return;
		}

		if ("payment_intent.succeeded".equals(eventType)) {
			markPaymentPaid(payment, providerPaymentId, eventId, null);
		}
		else if ("payment_intent.payment_failed".equals(eventType)) {
			String failure = objectNode.path("last_payment_error").path("message").asText(null);
			markPaymentFailed(payment, providerPaymentId, eventId, failure);
		}
		else if ("payment_intent.requires_capture".equals(eventType)
				|| "payment_intent.amount_capturable_updated".equals(eventType)) {
			markPaymentAuthorized(payment, providerPaymentId, eventId);
		}
	}

	@Transactional
	public void handleRazorpayWebhook(String signature, String eventId, String payload) {
		if (!verifyRazorpaySignature(signature, payload)) {
			throw new AuthorizationDeniedException("Invalid Razorpay webhook signature");
		}
		if (eventId != null && paymentRepository.existsByWebhookEventId(eventId)) {
			return;
		}

		JsonNode root = parsePayload(payload);
		String eventType = root.path("event").asText("");
		JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
		String providerPaymentId = paymentEntity.path("id").asText(null);
		String providerOrderId = paymentEntity.path("order_id").asText(null);

		Payment payment = paymentRepository
				.findByProviderAndProviderPaymentId(PaymentProvider.RAZORPAY, providerPaymentId)
				.or(() -> paymentRepository.findByProviderAndProviderOrderId(PaymentProvider.RAZORPAY, providerOrderId))
				.orElse(null);
		if (payment == null) {
			log.warn("Razorpay webhook ignored because payment could not be mapped");
			return;
		}

		if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
			markPaymentPaid(payment, providerPaymentId, eventId, providerOrderId);
		}
		else if ("payment.failed".equals(eventType)) {
			String failure = paymentEntity.path("error_description").asText(null);
			markPaymentFailed(payment, providerPaymentId, eventId, failure);
		}
		else if ("payment.authorized".equals(eventType)) {
			markPaymentAuthorized(payment, providerPaymentId, eventId);
		}
	}

	private ProviderPaymentInit createStripePaymentIntent(Payment payment) {
		if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
			return new ProviderPaymentInit(
					"pi_mock_" + payment.getId(),
					"pi_mock_" + payment.getId(),
					"pi_secret_mock_" + payment.getId(),
					PaymentStatus.PENDING);
		}

		try {
			long amountInMinor = toMinorUnit(payment.getAmount());
			String body = "amount=" + amountInMinor
					+ "&currency=" + urlEncode(payment.getCurrency().toLowerCase())
					+ "&metadata[order_id]=" + payment.getOrder().getId()
					+ "&metadata[payment_id]=" + payment.getId()
					+ "&automatic_payment_methods[enabled]=true";

			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/payment_intents"))
					.header("Authorization", "Bearer " + stripeSecretKey)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				log.error("Stripe initiation failed: status={} body={}", response.statusCode(), response.body());
				throw new IllegalArgumentException("Stripe payment initiation failed");
			}

			JsonNode root = objectMapper.readTree(response.body());
			String id = root.path("id").asText();
			String clientSecret = root.path("client_secret").asText(null);
			return new ProviderPaymentInit(id, id, clientSecret, PaymentStatus.PENDING);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to create Stripe payment intent", ex);
		}
	}

	private ProviderPaymentInit createRazorpayOrder(Payment payment) {
		if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
			return new ProviderPaymentInit(
					"order_mock_" + payment.getId(),
					null,
					null,
					PaymentStatus.PENDING);
		}

		try {
			long amountInMinor = toMinorUnit(payment.getAmount());
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("amount", amountInMinor);
			requestBody.put("currency", payment.getCurrency());
			requestBody.put("receipt", "order_" + payment.getOrder().getId() + "_pay_" + payment.getId());
			requestBody.put("notes", Map.of(
					"order_id", payment.getOrder().getId().toString(),
					"payment_id", payment.getId().toString()));

			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
					.header("Authorization", "Basic " + basicAuth(razorpayKeyId, razorpayKeySecret))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				log.error("Razorpay initiation failed: status={} body={}", response.statusCode(), response.body());
				throw new IllegalArgumentException("Razorpay payment initiation failed");
			}

			JsonNode root = objectMapper.readTree(response.body());
			String orderId = root.path("id").asText();
			return new ProviderPaymentInit(orderId, null, null, PaymentStatus.PENDING);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to create Razorpay order", ex);
		}
	}

	private Payment resolveStripePayment(JsonNode root, JsonNode objectNode, String providerPaymentId) {
		String metadataPaymentId = objectNode.path("metadata").path("payment_id").asText(null);
		if (metadataPaymentId != null && !metadataPaymentId.isBlank()) {
			try {
				Long paymentId = Long.valueOf(metadataPaymentId);
				return paymentRepository.findById(paymentId).orElse(null);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.STRIPE, providerPaymentId).orElse(null);
	}

	private void markPaymentAuthorized(Payment payment, String providerPaymentId, String webhookEventId) {
		payment.setStatus(PaymentStatus.AUTHORIZED);
		payment.setProviderPaymentId(providerPaymentId);
		payment.setWebhookEventId(webhookEventId);
		paymentRepository.save(payment);
	}

	private void markPaymentFailed(
			Payment payment, String providerPaymentId, String webhookEventId, String failureReason) {
		payment.setStatus(PaymentStatus.FAILED);
		payment.setProviderPaymentId(providerPaymentId);
		payment.setWebhookEventId(webhookEventId);
		payment.setFailureReason(failureReason);
		paymentRepository.save(payment);
	}

	private void markPaymentPaid(
			Payment payment, String providerPaymentId, String webhookEventId, String providerOrderId) {
		payment.setStatus(PaymentStatus.PAID);
		payment.setProviderPaymentId(providerPaymentId);
		payment.setWebhookEventId(webhookEventId);
		payment.setProviderOrderId(providerOrderId == null ? payment.getProviderOrderId() : providerOrderId);
		payment.setPaidAt(Instant.now());
		Payment saved = paymentRepository.save(payment);
		syncOrderAfterPayment(saved);
	}

	private void syncOrderAfterPayment(Payment payment) {
		FoodOrder order = payment.getOrder();
		if (order.getStatus() == OrderStatus.PENDING) {
			orderStateMachineService.assertTransitionAllowed(order.getStatus(), OrderStatus.CONFIRMED);
			order.setStatus(OrderStatus.CONFIRMED);
			foodOrderRepository.save(order);
			notificationService.notifyCustomerOfOrderStatusChange(order);
		}
	}

	private void assertCanInitiatePayment(User actor, FoodOrder order) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (actor.getRole() == Role.CUSTOMER && order.getCustomer().getId().equals(actor.getId())) {
			return;
		}
		throw new AuthorizationDeniedException("Only the order customer can initiate payment");
	}

	private FoodOrder findOrder(Long orderId) {
		return foodOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private String normalizeCurrency(String currency) {
		if (currency == null || currency.isBlank()) {
			return "INR";
		}
		return currency.trim().toUpperCase();
	}

	private long toMinorUnit(BigDecimal amount) {
		return amount.multiply(BigDecimal.valueOf(100L)).longValueExact();
	}

	private PaymentResponse toResponse(Payment payment) {
		return PaymentResponse.builder()
				.paymentId(payment.getId())
				.orderId(payment.getOrder().getId())
				.amount(payment.getAmount())
				.currency(payment.getCurrency())
				.provider(payment.getProvider())
				.method(payment.getMethod())
				.status(payment.getStatus())
				.providerOrderId(payment.getProviderOrderId())
				.providerPaymentId(payment.getProviderPaymentId())
				.clientSecret(payment.getProviderClientSecret())
				.paidAt(payment.getPaidAt())
				.build();
	}

	private JsonNode parsePayload(String payload) {
		try {
			return objectMapper.readTree(payload);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Invalid webhook payload", ex);
		}
	}

	private boolean verifyStripeSignature(String signatureHeader, String payload) {
		if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
			return true;
		}
		if (signatureHeader == null || signatureHeader.isBlank()) {
			return false;
		}

		String timestamp = null;
		String[] signatureValues = signatureHeader.split(",");
		for (String part : signatureValues) {
			String[] keyValue = part.split("=", 2);
			if (keyValue.length != 2) {
				continue;
			}
			if ("t".equals(keyValue[0])) {
				timestamp = keyValue[1];
			}
		}
		if (timestamp == null) {
			return false;
		}

		String signedPayload = timestamp + "." + payload;
		String expected = hmacSha256Hex(stripeWebhookSecret, signedPayload);
		for (String part : signatureValues) {
			String[] keyValue = part.split("=", 2);
			if (keyValue.length == 2 && "v1".equals(keyValue[0]) && secureEquals(expected, keyValue[1])) {
				return true;
			}
		}
		return false;
	}

	private boolean verifyRazorpaySignature(String signature, String payload) {
		if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
			return true;
		}
		if (signature == null || signature.isBlank()) {
			return false;
		}
		String expected = hmacSha256Hex(razorpayWebhookSecret, payload);
		return secureEquals(expected, signature);
	}

	private String hmacSha256Hex(String secret, String payload) {
		try {
			Mac sha256 = Mac.getInstance("HmacSHA256");
			sha256.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return toHex(digest);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Could not compute HMAC signature", ex);
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	private boolean secureEquals(String left, String right) {
		return MessageDigest.isEqual(
				left.getBytes(StandardCharsets.UTF_8),
				right.getBytes(StandardCharsets.UTF_8));
	}

	private String basicAuth(String username, String password) {
		String credentials = username + ":" + password;
		return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private record ProviderPaymentInit(
			String providerOrderId,
			String providerPaymentId,
			String clientSecret,
			PaymentStatus status) {
	}
}
