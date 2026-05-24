package com.fooddash.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentProviderConverter implements AttributeConverter<PaymentProvider, String> {

	@Override
	public String convertToDatabaseColumn(PaymentProvider attribute) {
		return attribute == null ? null : attribute.getDbValue();
	}

	@Override
	public PaymentProvider convertToEntityAttribute(String dbData) {
		return dbData == null ? null : PaymentProvider.fromDbValue(dbData);
	}
}
