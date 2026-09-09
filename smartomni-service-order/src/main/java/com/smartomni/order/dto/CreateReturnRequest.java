package com.smartomni.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReturnRequest {
    @NotNull
    private Long orderId;

    @NotBlank
    private String reason;
}
