package com.abw12.absolutefitness.ordermgmtms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class WebhookEventRes {

    private String accountId;
    private List<String> contains;
    private long createdAt;
    private String entity;
    private String event;
    private Map<String, Object> payload; // Dynamic payload
}
