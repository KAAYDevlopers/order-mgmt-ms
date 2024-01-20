package com.abw12.absolutefitness.ordermgmtms.advice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDetails {
    private String code;
    private String description;
    private String source;
    private String step;
    private String reason;
    private Map<String, Object> metadata;
    private String field;
}
