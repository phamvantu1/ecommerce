package com.electro.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderVariantKeyRequest {
    private Long orderId;
    private Long variantId;
}
