package com.abw12.absolutefitness.ordermgmtms.gateway.interfaces;

import com.abw12.absolutefitness.ordermgmtms.dto.request.InventoryUpdateReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.InventoryValidationReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Objects;

@FeignClient(name ="product-inventory-controller")
public interface ProductCatalogInventoryClient {

    @PostMapping("/saveInventoryData")
    ResponseEntity<Map<String, Objects>> updateInventoryData(@RequestBody InventoryUpdateReqDTO request);

    @GetMapping("/checkStockStatus")
    ResponseEntity<Map<String, Objects>> cartValidation(@RequestBody InventoryValidationReq request);
}
