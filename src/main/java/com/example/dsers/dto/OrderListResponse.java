package com.example.dsers.dto;

import lombok.Data;

import java.util.List;

/**
 * 接收 Shopify 返回的订单列表
 * Shopify 返回格式：{"orders": [订单1, 订单2, ...]}
 */
@Data
public class OrderListResponse {
    private List<ShopifyOrderDto> orders;
}
