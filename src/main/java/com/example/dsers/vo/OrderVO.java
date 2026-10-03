package com.example.dsers.vo;

import com.example.dsers.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class OrderVO {
    private Long id;                 // 订单 id（查详情用）
    private String orderNumber;      // 订单号
    private String email;            // 买家邮箱
    private BigDecimal totalPrice;   // 金额
    private String currency;         // 币种
    private String financialStatus;  // 支付状态
    private String fulfillmentStatus;// 发货状态
    private LocalDateTime createdAt; // 下单时间

    // ==================== 阶段1新增 ====================
    private String platform;         // 来源平台：SHOPIFY / WOOCOMMERCE
    private String status;           // 本系统中台的状态：PENDING/ORDERED/SHIPPED/SYNCED
    private String trackingNumber;   // 运单号

    private List<OrderItem> items;   // 商品明细列表
}
