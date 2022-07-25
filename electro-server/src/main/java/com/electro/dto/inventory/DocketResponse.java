package com.electro.dto.inventory;

import com.electro.dto.order.Order_DocketResponse;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class DocketResponse {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer type;
    private String code;
    private DocketReasonResponse reason;
    private WarehouseResponse warehouse;
    private Set<DocketVariantResponse> docketVariants;
    private PurchaseOrder_DocketResponse purchaseOrder;
    private Order_DocketResponse order;
    private String note;
    private Integer status;

}
