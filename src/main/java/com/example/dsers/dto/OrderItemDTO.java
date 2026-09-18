package com.example.dsers.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    // 对应 Shopify 返回的商品明细，字段完全一致
    private Long id;
    private String title;
    private Integer quantity;
    private String price;
    private String sku;
}
