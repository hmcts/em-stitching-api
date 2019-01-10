package uk.gov.hmcts.reform.em.stitching.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
public class BundleToStringMapper implements AttributeConverter<Bundle, String> {

    ObjectMapper mapper = new ObjectMapper();

    public BundleToStringMapper() {
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String convertToDatabaseColumn(Bundle data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Bundle convertToEntityAttribute(String data) {
        TypeReference<Bundle> typeRef = new TypeReference<Bundle>() {};

        try {
            return mapper.readValue(data, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}