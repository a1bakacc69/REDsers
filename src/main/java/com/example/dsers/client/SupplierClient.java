package com.example.dsers.client;

import com.example.dsers.dto.SupplierOrderQueryResponse;
import com.example.dsers.dto.SupplierOrderRequest;
import com.example.dsers.dto.SupplierOrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 负责跟供货商平台打交道：把订单发过去，拿回对方的订单号。
 * 只发请求、只取单号，不碰数据库 —— 写库是调它的 Service 的事。
 */
@Component
public class SupplierClient {

    @Value("${supplier.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String placeOrder(SupplierOrderRequest req) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SupplierOrderRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<SupplierOrderResponse> response = restTemplate.exchange(
                baseUrl + "/supplier/orders",
                HttpMethod.POST,
                entity,
                SupplierOrderResponse.class
        );

        return response.getBody().getData().getSupplierOrderId();
    }

    // 查供货商那边的发货状态
    public SupplierOrderQueryResponse.Data queryOrder(String supplierOrderId) {

        String url = baseUrl + "/supplier/orders/" + supplierOrderId;

        ResponseEntity<SupplierOrderQueryResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, null, SupplierOrderQueryResponse.class
        );

        return response.getBody().getData();
    }

}

