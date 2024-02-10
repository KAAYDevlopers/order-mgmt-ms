package com.abw12.absolutefitness.ordermgmtms.gateway.interfaces;

import com.abw12.absolutefitness.ordermgmtms.config.FeignClientConfig;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VariantInventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name ="product-inventory-controller" , configuration = FeignClientConfig.class)
public interface ProductCatalogInventoryClient {

    @PostMapping("/saveInventoryData")
    ResponseEntity<Map<String, Object>> saveInventoryData(@RequestBody VariantInventoryDTO request);

    @GetMapping("/getVariantInventory/{variantId}")
    ResponseEntity<Map<String,Object>> getVariantInventoryData(@PathVariable String variantId);

    @GetMapping("/checkStockStatus")
    ResponseEntity<Map<String, Object>> cartValidation(@RequestParam Map<String,Object> request);

    @RequestMapping(method = RequestMethod.PATCH,value = "/updateInventoryData")
    ResponseEntity<Map<String,Object>> patchVariantInventoryData(@RequestBody Map<String,Object> params);
}
