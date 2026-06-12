package com.ai.Resume.analyser.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Converter
public class JdCategoryBreakdownConverter implements AttributeConverter<Map<String, JdMatchResponse.CategoryScore>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, JdMatchResponse.CategoryScore> attribute) {
        if (attribute == null) return null;
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public Map<String, JdMatchResponse.CategoryScore> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        try {
            return mapper.readValue(dbData, new TypeReference<Map<String, JdMatchResponse.CategoryScore>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
