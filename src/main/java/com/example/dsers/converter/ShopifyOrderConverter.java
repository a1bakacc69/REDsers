package com.example.dsers.converter;

import com.example.dsers.dto.AddressDto;
import com.example.dsers.dto.OrderItemDTO;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.entity.OrderItem;
import com.example.dsers.entity.PlatformOrder;
import com.example.dsers.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 把 Shopify 的 DTO 翻译成实体。
 * 只做翻译，不碰数据库；只映射 A 区（平台镜像）字段。
 */
@Component
public class ShopifyOrderConverter {

    /** 当前接入的平台标识（接入第二个平台时抽成枚举） */
    private static final String PLATFORM_SHOPIFY = "SHOPIFY";

    // ============================================================
    // 主表：ShopifyOrderDto → PlatformOrder
    // ============================================================

    public PlatformOrder toEntity(ShopifyOrderDto dto) {
        PlatformOrder order = new PlatformOrder();

        // Shopify 的 id 是平台侧订单ID，我们叫 platformOrderId
        order.setPlatformOrderId(dto.getId());

        order.setOrderNumber(dto.getOrderNumber());
        order.setName(dto.getName());
        order.setEmail(dto.getEmail());
        order.setCurrency(dto.getCurrency());
        order.setFinancialStatus(dto.getFinancialStatus());
        order.setFulfillmentStatus(dto.getFulfillmentStatus());

        order.setTotalPrice(toDecimal(dto.getTotalPrice()));
        order.setSubtotalPrice(toDecimal(dto.getSubtotalPrice()));
        order.setTotalTax(toDecimal(dto.getTotalTax()));

        order.setCreatedAt(toDateTime(dto.getCreatedAt()));
        order.setCancelledAt(toDateTime(dto.getCancelledAt()));

        // 先掏出来存局部变量，省得每行重复 getShippingAddress()，也只需在门口判一次空
        AddressDto shipping = dto.getShippingAddress();

        if (shipping != null) {
            // 姓名要三级兜底，见文件底部 resolveShippingName
            order.setShippingName(resolveShippingName(dto));

            order.setShippingPhone(shipping.getPhone());
            order.setShippingAddress1(shipping.getAddress1());
            order.setShippingAddress2(shipping.getAddress2());
            order.setShippingCity(shipping.getCity());
            order.setShippingZip(shipping.getZip());
            order.setShippingProvince(shipping.getProvince());
            order.setShippingCountryCode(shipping.getCountryCode());
        }
        // 没地址就存 null，不编造数据

        order.setPlatform(PLATFORM_SHOPIFY);
        order.setStatus(OrderStatus.PENDING.name());

        return order;
    }

    // ============================================================
    // 明细：OrderItemDTO → OrderItem
    // ============================================================

    // orderId 要用主表插入后回填的那个 id，不是 dto 里的
    public OrderItem toItemEntity(OrderItemDTO dto, Long orderId) {
        OrderItem item = new OrderItem();

        item.setOrderId(orderId);
        item.setPlatformLineItemId(dto.getId());   // 改名：Shopify 的 line_item.id
        item.setTitle(dto.getTitle());
        item.setQuantity(dto.getQuantity());
        item.setPrice(toDecimal(dto.getPrice()));
        item.setSku(dto.getSku());

        // 回写平台时靠这三个说清楚发的是哪个规格
        item.setVariantId(dto.getVariantId());
        item.setProductId(dto.getProductId());
        item.setVariantTitle(dto.getVariantTitle());

        return item;
    }

    // ============================================================
    // 工具方法
    // ============================================================

    // 三级兜底：shipping.name → billing.name → first_name + last_name。
    // Shopify 实测返回的是空串 "" 而不是 null，只判 null 挡不住，空串会溜进库里，寄件时才发现是空的
    private String resolveShippingName(ShopifyOrderDto dto) {
        AddressDto shipping = dto.getShippingAddress();

        if (shipping != null && isNotBlank(shipping.getName())) {
            return shipping.getName();
        }

        // 实测这单就是从这里拿到的
        AddressDto billing = dto.getBillingAddress();
        if (billing != null && isNotBlank(billing.getName())) {
            return billing.getName();
        }

        if (shipping != null && isNotBlank(shipping.getFirstName())) {
            String lastName = shipping.getLastName() == null ? "" : shipping.getLastName();
            return (shipping.getFirstName() + " " + lastName).trim();
        }

        return null;
    }

    // 必须同时挡 null 和空串 —— Shopify 给的是 "" 不是 null
    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    // 空值返回 null 不返回 0 —— 「金额未知」和「金额是 0」是两回事
    private BigDecimal toDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    // Shopify 给的是带时区的 ISO 串，先 OffsetDateTime.parse 再去掉时区。
    // 空值返回 null —— cancelled_at 基本都是 null
    private LocalDateTime toDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }
}
