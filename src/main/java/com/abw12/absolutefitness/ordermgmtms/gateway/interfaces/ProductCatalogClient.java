package com.abw12.absolutefitness.ordermgmtms.gateway.interfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "product-catalog-controller")
public interface ProductCatalogClient {

    @GetMapping("/getVariantData/{variantId}")
    ResponseEntity<Map<String,Object>> getProductVariantById(@PathVariable String variantId);
}
