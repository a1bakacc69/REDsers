package com.example.dsers.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shopify_order")
public class ShopifyOrder {
    @TableId(type = IdType.AUTO)
    private Long id;//自动生成

    private Long shopifyOrderId;//订单id，拿到数据有

    private String orderNumber;

    private String name;

    private String email;

    private BigDecimal totalPrice;

    private BigDecimal subtotalPrice;

    private BigDecimal totalTax;

    private String currency;

    private String financialStatus;//

    private String fulfillmentStatus;

    private LocalDateTime createdAt;//
    private LocalDateTime cancelledAt;
}
