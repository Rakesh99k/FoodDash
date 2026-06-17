package com.fooddash.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {

	@Override
	protected Principal determineUser(
			ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
		Object principal = attributes.get("principal");
		if (principal instanceof Principal userPrincipal) {
			return userPrincipal;
		}
		return super.determineUser(request, wsHandler, attributes);
	}
}
