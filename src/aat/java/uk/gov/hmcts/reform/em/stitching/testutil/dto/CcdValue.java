package uk.gov.hmcts.reform.em.stitching.testutil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CcdValue<T> implements Serializable {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public CcdValue(T t) {
        setValue(t);
    }

    public CcdValue(){}
}
