package com.example.dsers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Shopify「履约单列表」的响应。
 *
 * <p>对应接口：{@code GET /admin/api/2024-07/orders/{order_id}/fulfillment_orders.json}
 *
 * <p><b>★ 字段是「抄」来的，不是想出来的 ★</b>
 * 下面这段是真实调接口拿到的响应（省略了用不上的字段）：
 * <pre>
 * {"fulfillment_orders":[{
 *    "id": 7646783111226,                       ← ⭕ 我们要的就是它
 *    "created_at": "2026-09-21T02:20:17-04:00",
 *    "shop_id": 60341682234,
 *    "order_id": 6465733001274,
 *    "status": "open",                          ← ⭕ 顺手接一下
 *    "supported_actions": ["create_fulfillment","hold","split"],
 *    "destination": {...},
 *    "line_items": [{...}, {...}]
 * }]}
 * </pre>
 *
 * <p>划圈的标准只有一句话：<b>这个值，我后面哪行代码会用到？</b>
 * <ul>
 *   <li>{@code id} → 回写运单时要当参数传给 Shopify，必须留</li>
 *   <li>{@code status} → 排查问题时看一眼（open / in_progress / closed），留着省事</li>
 *   <li>其余 90% 的字段这辈子都不会碰，不写。Jackson 会静默丢弃没声明的字段，不会报错</li>
 * </ul>
 */
@Data
public class FulfillmentOrderListResponse {

    /** JSON 里的名字是蛇形 fulfillment_orders，Java 里按驼峰写，靠这个注解对应上 */
    @JsonProperty("fulfillment_orders")
    private List<FulfillmentOrder> fulfillmentOrders;

    @Data
    public static class FulfillmentOrder {

        /** ★ 唯一必须的字段：履约单 id */
        private Long id;

        /** open / in_progress / closed */
        private String status;
    }
}
