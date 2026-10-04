package com.example.dsers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Shopify「履约单列表」响应（GET /orders/{order_id}/fulfillment_orders.json）。
 * 字段是照真实响应抄的，不是拍脑袋想的：只留 id 和 status —— 前者回写运单要当参数，
 * 后者排查时看一眼。其余不写，Jackson 会静默丢掉没声明的字段。
 */
@Data
public class FulfillmentOrderListResponse {

    /** JSON 里是蛇形 fulfillment_orders，靠注解对上驼峰字段名 */
    @JsonProperty("fulfillment_orders")
    private List<FulfillmentOrder> fulfillmentOrders;

    @Data
    public static class FulfillmentOrder {

        /** 回写运单时要传给 Shopify */
        private Long id;

        /** open / in_progress / closed */
        private String status;
    }
}
