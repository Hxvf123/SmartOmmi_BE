package com.smartomni.order.client;

import com.smartomni.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Goi sang smartomni-service-inventory de tru kho khi don hang moi duoc tao (UC-33). */
@FeignClient(name = "smartomni-service-inventory", url = "${smartomni.services.inventory-url:http://localhost:8084}")
public interface InventoryServiceClient {

    @PostMapping("/api/inventory/{skuId}/deduct")
    ApiResponse<Void> deductStock(@PathVariable("skuId") Long skuId, @RequestParam("quantity") int quantity);
}
