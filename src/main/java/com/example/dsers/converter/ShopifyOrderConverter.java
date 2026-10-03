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
 * Shopify 数据转换器：把 Shopify 的 DTO「翻译」成我们的实体。
 * <p>
 * 职责边界：<b>只做翻译，不碰数据库</b>（保存是 Service 的事）。
 * <p>
 * 只映射 A 区（平台镜像）字段 —— 见《设计文档》5.2 三个分区。
 */
@Component
public class ShopifyOrderConverter {

    /** 当前接入的平台标识（接入第二个平台时抽成枚举） */
    private static final String PLATFORM_SHOPIFY = "SHOPIFY";

    // ============================================================
    // 主表：ShopifyOrderDto → PlatformOrder
    // ============================================================

    /**
     * DTO → 订单主表实体
     */
    public PlatformOrder toEntity(ShopifyOrderDto dto) {
        PlatformOrder order = new PlatformOrder();

        // ① 改名映射：Shopify 的 id 是「平台侧订单ID」，我们叫 platformOrderId
        order.setPlatformOrderId(dto.getId());

        // ② 同名直传
        order.setOrderNumber(dto.getOrderNumber());
        order.setName(dto.getName());
        order.setEmail(dto.getEmail());
        order.setCurrency(dto.getCurrency());
        order.setFinancialStatus(dto.getFinancialStatus());
        order.setFulfillmentStatus(dto.getFulfillmentStatus());

        // ③ 金额：String → BigDecimal
        order.setTotalPrice(toDecimal(dto.getTotalPrice()));
        order.setSubtotalPrice(toDecimal(dto.getSubtotalPrice()));
        order.setTotalTax(toDecimal(dto.getTotalTax()));

        // ④ 时间：String → LocalDateTime
        order.setCreatedAt(toDateTime(dto.getCreatedAt()));
        order.setCancelledAt(toDateTime(dto.getCancelledAt()));

        // ⑤ 收货地址：嵌套对象 → 8 个扁平字段
        // 先把嵌套对象「掏出来」存到局部变量：这样后面每行不用重复写 getShippingAddress()，
        // 而且只需要在门口判一次空
        AddressDto shipping = dto.getShippingAddress();

        if (shipping != null) {
            // 姓名单独处理：三级兜底，抽成独立方法（见文件底部）
            order.setShippingName(resolveShippingName(dto));

            // 剩下 7 个：纯机械搬运，一行一个
            order.setShippingPhone(shipping.getPhone());
            order.setShippingAddress1(shipping.getAddress1());
            order.setShippingAddress2(shipping.getAddress2());
            order.setShippingCity(shipping.getCity());
            order.setShippingZip(shipping.getZip());
            order.setShippingProvince(shipping.getProvince());
            order.setShippingCountryCode(shipping.getCountryCode());
        }
        // 若 shipping == null：8 个字段保持 null。
        // 「没有地址」就存 null —— 不编造数据

        // ⑥ 常量赋值
        order.setPlatform(PLATFORM_SHOPIFY);
        order.setStatus(OrderStatus.PENDING.name());

        return order;
    }

    // ============================================================
    // 明细：OrderItemDTO → OrderItem
    // ============================================================

    /**
     * DTO → 订单明细实体
     *
     * @param dto     平台返回的明细
     * @param orderId 主表刚插入后回填的 id（★ 用这个，不是 dto 里的）
     */
    public OrderItem toItemEntity(OrderItemDTO dto, Long orderId) {
        OrderItem item = new OrderItem();

        item.setOrderId(orderId);
        item.setPlatformLineItemId(dto.getId());   // ★ 改名：Shopify 的 line_item.id
        item.setTitle(dto.getTitle());
        item.setQuantity(dto.getQuantity());
        item.setPrice(toDecimal(dto.getPrice()));
        item.setSku(dto.getSku());

        // 商品标识：三个都是同名直传（DTO 和实体字段名一模一样）
        // 它们的作用：回写平台时说清楚「发的是哪一个规格」
        item.setVariantId(dto.getVariantId());
        item.setProductId(dto.getProductId());
        item.setVariantTitle(dto.getVariantTitle());

        return item;
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 解析收货人姓名 —— <b>三级兜底</b>。
     *
     * <p><b>为什么需要兜底？</b>
     * Shopify 的 {@code shipping_address.name} 实测是<b>空字符串 ""</b>，
     * 不是 null。只判 null 的话，空串会溜过去，数据库里存进一堆 ""——
     * 打印出来看不见，但寄件时是空的，会寄不出去。
     *
     * <p>兜底顺序：
     * <ol>
     *   <li>{@code shipping_address.name}    —— 首选</li>
     *   <li>{@code billing_address.name}     —— 实测这里能拿到 "Russell Winfield"</li>
     *   <li>{@code first_name + " " + last_name} —— 最后拼一个</li>
     * </ol>
     *
     * @return 姓名；三级都拿不到时返回 null（"没有"就是"没有"，不编造）
     */
    private String resolveShippingName(ShopifyOrderDto dto) {
        AddressDto shipping = dto.getShippingAddress();

        // ① 首选：收货地址的姓名
        if (shipping != null && isNotBlank(shipping.getName())) {
            return shipping.getName();
        }

        // ② 次选：账单地址的姓名（★ 实测这单就是从这里拿到的）
        AddressDto billing = dto.getBillingAddress();
        if (billing != null && isNotBlank(billing.getName())) {
            return billing.getName();
        }

        // ③ 兜底：first_name + last_name 拼一个
        if (shipping != null && isNotBlank(shipping.getFirstName())) {
            String lastName = shipping.getLastName() == null ? "" : shipping.getLastName();
            return (shipping.getFirstName() + " " + lastName).trim();
        }

        return null;
    }

    /**
     * 判空 —— <b>必须同时挡住 null 和空字符串</b>。
     *
     * <p>Shopify 给的是 {@code ""} 而不是 null，
     * 所以 {@code value != null} 挡不住它，必须再加 {@code !isBlank()}。
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * String → BigDecimal，安全转换。
     * <p>
     * 空值返回 null，<b>不返回 0</b> —— 「金额未知」和「金额是 0」是两回事。
     */
    private BigDecimal toDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    /**
     * ISO 时间字符串 → LocalDateTime，安全转换。
     * <p>
     * Shopify 给的是带时区的格式（2026-09-06T12:34:56-04:00），
     * 所以要先 OffsetDateTime.parse()，再去掉时区。
     * <p>
     * 空值返回 null —— cancelled_at 绝大多数时候都是 null。
     */
    private LocalDateTime toDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }
}
