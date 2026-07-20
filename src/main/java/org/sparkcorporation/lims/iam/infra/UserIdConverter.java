package org.sparkcorporation.lims.iam.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.api.UserId;

@Converter(autoApply = false)
public class UserIdConverter implements AttributeConverter<UserId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable UserId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable UserId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new UserId(dbData);
    }
}
