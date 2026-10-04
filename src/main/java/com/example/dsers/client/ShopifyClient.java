package com.example.dsers.client;

import com.example.dsers.dto.FulfillmentOrderListResponse;
import com.example.dsers.dto.OrderListResponse;
import com.example.dsers.dto.ShopifyOrderDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责跟 Shopify 打交道：主动去拉订单
 */
@Component
public class ShopifyClient {

    @Value("${shopify.store.url}")
    private String storeUrl;

    @Value("${shopify.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<ShopifyOrderDto> fetchOrders() {
        String url = storeUrl + "/admin/api/2024-07/orders.json?status=any&limit=250";

        // Shopify 靠这个 header 认证
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<OrderListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, OrderListResponse.class
        );

        OrderListResponse body = response.getBody();
        if (body == null || body.getOrders() == null) {
            return List.of();
        }
        return body.getOrders();
    }

    // 回写运单号要的是 fulfillment_order 的 id，不是我们库里的 order id，所以得先查一次。
    // 查不到返回 null，交给 Service 当错误处理
    public Long fetchFulfillmentOrderId(Long platformOrderId) {

        String url = storeUrl
                + "/admin/api/2024-07/orders/" + platformOrderId + "/fulfillment_orders.json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<FulfillmentOrderListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, FulfillmentOrderListResponse.class
        );

        FulfillmentOrderListResponse body = response.getBody();
        if (body == null
                || body.getFulfillmentOrders() == null
                || body.getFulfillmentOrders().isEmpty()) {
            return null;
        }
        // 一个订单可能拆成多张发货单，这里只取第一张
        return body.getFulfillmentOrders().get(0).getId();
    }

    // notify_customer 必须设 false —— 否则 Shopify 会给真实买家发发货邮件。
    // Map 的 key 就是 JSON 字段名，得按 Shopify 的蛇形写法，不能按 Java 习惯写驼峰
    public void createFulfillment(Long fulfillmentOrderId,
                                  String trackingNumber,
                                  String trackingCompany) {

        String url = storeUrl + "/admin/api/2024-07/fulfillments.json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> trackingInfo = new HashMap<>();
        trackingInfo.put("number", trackingNumber);
        trackingInfo.put("company", trackingCompany);

        Map<String, Object> itemByOrder = new HashMap<>();
        itemByOrder.put("fulfillment_order_id", fulfillmentOrderId);

        Map<String, Object> fulfillment = new HashMap<>();
        fulfillment.put("line_items_by_fulfillment_order", List.of(itemByOrder));
        fulfillment.put("tracking_info", trackingInfo);
        fulfillment.put("notify_customer", false);

        Map<String, Object> body = new HashMap<>();
        body.put("fulfillment", fulfillment);

        restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class
        );
    }
}
