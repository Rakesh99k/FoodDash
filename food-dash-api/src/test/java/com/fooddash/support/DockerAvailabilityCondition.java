package com.fooddash.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

public class DockerAvailabilityCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		if (DockerClientFactory.instance().isDockerAvailable()) {
			return ConditionEvaluationResult.enabled("Docker is available");
		}
		return ConditionEvaluationResult.disabled("Docker is unavailable in this environment");
	}
}
