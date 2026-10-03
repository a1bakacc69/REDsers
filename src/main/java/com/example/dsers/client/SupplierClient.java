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
 *
 * <p>注意职责边界：这个类只发请求、只取单号，不碰数据库。
 * 写库是调它的 Service 的事。
 */
@Component
public class SupplierClient {

    @Value("${supplier.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 向供货商下单。
     * @return 供货商侧的订单号，如 SUP-1790839310687
     */
    public String placeOrder(SupplierOrderRequest req) {

        // 【1】请求头：声明「我发的 body 是 JSON」
        //     MediaType.APPLICATION_JSON 就是 HTTP 头 Content-Type: application/json 的 Java 写法
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 【2】把「要发的东西」和「请求头」打包
        //     对照 ShopifyClient：那边是 new HttpEntity<>(headers)，因为不带 body
        //     这里多了一个参数 —— req 就是 body
        HttpEntity<SupplierOrderRequest> entity = new HttpEntity<>(req, headers);

        // 【3】POST 出去
        //     四个参数：地址、方法、包、返回的类型
        ResponseEntity<SupplierOrderResponse> response = restTemplate.exchange(
                baseUrl + "/supplier/orders",
                HttpMethod.POST,
                entity,
                SupplierOrderResponse.class
        );

        // 【4】拆包取值
        //     response.getBody() 拿到响应体对象，再 .getData().getSupplierOrderId() 取单号
        return response.getBody().getData().getSupplierOrderId();
    }

    //查询供货商那边的发货状态。
    public SupplierOrderQueryResponse.Data queryOrder(String supplierOrderId) {

        String url = baseUrl + "/supplier/orders/" + supplierOrderId;

        ResponseEntity<SupplierOrderQueryResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, null, SupplierOrderQueryResponse.class
        );

        return response.getBody().getData();
    }

}

