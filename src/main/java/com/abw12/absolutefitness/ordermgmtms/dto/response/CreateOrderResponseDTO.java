package com.abw12.absolutefitness.ordermgmtms.dto.response;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponseDTO {

    private Integer amount;
    private Integer amount_paid;
    private Map<String,String> notes;
    private Integer created_at;
    private Integer amount_due;
    private String currency;
    private String receipt;
    private String id;
    private String entity;
    private String offer_id;
    private String status;
    private Integer attempts;
}
