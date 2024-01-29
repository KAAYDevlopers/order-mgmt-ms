package com.abw12.absolutefitness.ordermgmtms.gateway.interfaces;

import com.abw12.absolutefitness.ordermgmtms.dto.request.InventoryValidationReq;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VariantInventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name ="product-inventory-controller")
public interface ProductCatalogInventoryClient {

    @PostMapping("/saveInventoryData")
    ResponseEntity<Map<String, Object>> updateInventoryData(@RequestBody VariantInventoryDTO request);

    @GetMapping("/getVariantInventory/{variantId}")
    ResponseEntity<Map<String,Object>> getVariantInventoryData(@PathVariable String variantId);

    @GetMapping("/checkStockStatus")
    ResponseEntity<Map<String, Object>> cartValidation(@RequestBody InventoryValidationReq request);
}
