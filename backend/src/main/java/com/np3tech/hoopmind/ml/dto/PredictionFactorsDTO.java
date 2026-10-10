package com.np3tech.hoopmind.ml.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionFactorsDTO {
    private Map<String, String> factors = new HashMap<>();

    public void addFactor(String key, String value) {
        factors.put(key, value);
    }

    public Map<String, String> getFactors() {
        return factors;
    }
}
