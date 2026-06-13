package com.fooddash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddash.model.AuditLog;
import com.fooddash.model.User;
import com.fooddash.repository.AuditLogRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(User actor, String action, String entityType, Long entityId, Object payload) {
		Long actorId = actor == null ? null : actor.getId();
		auditLogRepository.save(AuditLog.builder()
				.actorUserId(actorId)
				.action(action)
				.entityType(entityType)
				.entityId(entityId)
				.payload(serializePayload(payload))
				.build());
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordSystem(String action, String entityType, Long entityId, Object payload) {
		auditLogRepository.save(AuditLog.builder()
				.action(action)
				.entityType(entityType)
				.entityId(entityId)
				.payload(serializePayload(payload))
				.build());
	}

	private String serializePayload(Object payload) {
		if (payload == null) {
			return null;
		}
		if (payload instanceof String stringPayload) {
			return stringPayload;
		}
		if (payload instanceof Map<?, ?> mapPayload) {
			try {
				return objectMapper.writeValueAsString(mapPayload);
			}
			catch (Exception ex) {
				return mapPayload.toString();
			}
		}
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (Exception ex) {
			return payload.toString();
		}
	}
}
