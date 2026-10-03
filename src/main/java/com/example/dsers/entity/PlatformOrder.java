package com.example.dsers.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体（多平台通用）
 * 对应表：orders
 */
@Data
@TableName("orders")
public class PlatformOrder {

    @TableId(type = IdType.AUTO)
    private Long id;                    // 本地自增主键

    /** 平台侧订单ID（Shopify 的 order.id / WooCommerce 的 order.id） */
    private Long platformOrderId;

    private String orderNumber;         // 订单号（平台展示用）

    private String name;                // 订单显示名，如 #1001

    private String email;               // 买家邮箱

    private BigDecimal totalPrice;      // 订单总金额

    private BigDecimal subtotalPrice;   // 商品小计

    private BigDecimal totalTax;        // 税费

    private String currency;            // 币种

    // ==================== 阶段2新增：收货地址（下单给供应商用）====================

    /** 收件人姓名。⚠️ Shopify 的 shipping_address.name 可能为空，需从 billing_address.name 兜底 */
    private String shippingName;

    /** 收件人电话 */
    private String shippingPhone;

    /** 收货地址1，如街道 */
    private String shippingAddress1;

    /** 收货地址2，如门牌/单元 */
    private String shippingAddress2;

    /** 城市 */
    private String shippingCity;

    /** 邮编 */
    private String shippingZip;

    /** 省/州 */
    private String shippingProvince;

    /** 国家代码，如 CA（用代码不用名字，不受大小写和翻译影响） */
    private String shippingCountryCode;

    private String financialStatus;     // 平台侧支付状态

    private String fulfillmentStatus;   // 平台侧发货状态

    private LocalDateTime createdAt;    // 平台下单时间

    private LocalDateTime cancelledAt;  // 取消时间

    // ==================== 阶段1新增：多平台 + 状态机 + 履约 ====================

    /** 来源平台：SHOPIFY / WOOCOMMERCE ...
     *  与 platform_order_id 一起构成联合唯一键 uk_platform_order */
    private String platform;

    /** 订单状态：PENDING → ORDERED → SHIPPED → SYNCED（见 OrderStatus 枚举） */
    private String status;

    /** 运单号（供应商发货后回填） */
    private String trackingNumber;

    /** 承运商，如 顺丰 / DHL / UPS */
    private String trackingCompany;

    /** 供应商侧的订单号 */
    private String supplierOrderId;

    /** 回写平台是否失败：0 否 1 是 */
    private Integer syncFailed;
}
