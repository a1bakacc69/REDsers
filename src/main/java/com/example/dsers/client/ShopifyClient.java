package com.example.dsers.client;

import com.example.dsers.dto.OrderListResponse;
import com.example.dsers.dto.ShopifyOrderDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

    /**
     * 拉取 Shopify 的所有订单
     */
    public List<ShopifyOrderDto> fetchOrders() {
        // 拼请求地址：店铺地址 + 订单接口
        String url = storeUrl + "/admin/api/2024-07/orders.json";

        // 请求头带上 Token（Shopify 靠这个认证）
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 发 GET 请求，把返回的 JSON 自动转成 OrderListResponse
        ResponseEntity<OrderListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, OrderListResponse.class
        );

        // 拿到订单列表
        OrderListResponse body = response.getBody();
        if (body == null || body.getOrders() == null) {
            return List.of();  // 没有订单，返回空列表
        }
        return body.getOrders();
    }
}
