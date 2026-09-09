package com.smartomni.order.controller;

import com.smartomni.common.dto.ApiResponse;
import com.smartomni.order.dto.CreateReturnRequest;
import com.smartomni.order.entity.ReturnRefund;
import com.smartomni.order.service.ReturnRefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** UC-13: Quan ly hoan/huy don hang. */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnRefundController {

    private final ReturnRefundService returnRefundService;

    @PostMapping
    public ApiResponse<ReturnRefund> create(@Valid @RequestBody CreateReturnRequest request) {
        return ApiResponse.success("Da tao yeu cau hoan/huy", returnRefundService.createReturnRequest(request));
    }

    @PostMapping("/{refundId}/approve")
    public ApiResponse<ReturnRefund> approve(@PathVariable Long refundId) {
        return ApiResponse.success("Da duyet va cong tra ton kho", returnRefundService.approveAndRestock(refundId));
    }
}
