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

    /**
     * 拉取 Shopify 的所有订单
     */
    public List<ShopifyOrderDto> fetchOrders() {
        // 拼请求地址：店铺地址 + 订单接口
        String url = storeUrl + "/admin/api/2024-07/orders.json?status=any&limit=250";

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

    /**
     * 【第 1 步】查这个订单在 Shopify 那边的「履约单 id」。
     *
     * <p>为什么要多这一步？因为 Shopify 把「订单」拆成了三层：
     * <pre>
     *   order（订单，买家下的）
     *     └── fulfillment_order（发货单，仓库要发的）
     *           └── fulfillment（履约，实际发出的包裹，带运单号）
     * </pre>
     * 回写运单号时，Shopify 要的是 <b>fulfillment_order 的 id</b>，
     * 不是我们库里存的那个 order id。所以得先来问一句。
     *
     * <p>GET /admin/api/2024-07/orders/{order_id}/fulfillment_orders.json
     *
     * @param platformOrderId 平台订单 id（我们库里的 platform_order_id）
     * @return 履约单 id；查不到返回 null（Service 层会当成错误处理）
     */
    public Long fetchFulfillmentOrderId(Long platformOrderId) {

        String url = storeUrl
                + "/admin/api/2024-07/orders/" + platformOrderId + "/fulfillment_orders.json";

        // 和 fetchOrders 一模一样的三件套：请求头 + token + GET
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
        // 一个订单可能拆成多张发货单，我们这里只取第一张（简化处理）
        return body.getFulfillmentOrders().get(0).getId();
    }

    /**
     * 【第 2 步】在 Shopify 那边创建履约记录 —— 就是告诉平台「这个包裹发了」。
     *
     * <p>POST /admin/api/2024-07/fulfillments.json
     *
     * <p>这次是 POST，和拉订单有三个不同：
     * <ol>
     *   <li>请求头要多一句 {@code setContentType(APPLICATION_JSON)} ——
     *       因为这次<b>我们要发一个 JSON 过去</b>，得先告诉对方「我发的是 JSON」。
     *       GET 没有请求体，所以之前不用写这句。</li>
     *   <li>要用 {@code new HttpEntity<>(body, headers)} 把请求体塞进去 ——
     *       之前是 {@code new HttpEntity<>(headers)}，只有头没有体。</li>
     *   <li>请求体现在用 Map 一层层拼。Shopify 要求的格式是：
     *       <pre>
     * {"fulfillment": {
     *    "line_items_by_fulfillment_order": [{"fulfillment_order_id": 7646783111226}],
     *    "tracking_info": {"number": "231669096273", "company": "DHL"},
     *    "notify_customer": false
     * }}
     *       </pre>
     *       Map 的 key 就是 JSON 的字段名，所以这里必须写死成蛇形的
     *       {@code line_items_by_fulfillment_order} —— 不能按 Java 习惯写驼峰。</li>
     * </ol>
     */
    public void createFulfillment(Long fulfillmentOrderId,
                                  String trackingNumber,
                                  String trackingCompany) {

        String url = storeUrl + "/admin/api/2024-07/fulfillments.json";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);   // ★ 告诉对方：我发的是 JSON

        // 往下一层层拼：运单号 + 承运商
        Map<String, Object> trackingInfo = new HashMap<>();
        trackingInfo.put("number", trackingNumber);
        trackingInfo.put("company", trackingCompany);

        // 指定给哪张发货单履约
        Map<String, Object> itemByOrder = new HashMap<>();
        itemByOrder.put("fulfillment_order_id", fulfillmentOrderId);

        // 中间的 fulfillment 对象
        Map<String, Object> fulfillment = new HashMap<>();
        fulfillment.put("line_items_by_fulfillment_order", List.of(itemByOrder));
        fulfillment.put("tracking_info", trackingInfo);
        // ★ 别给买家发邮件。真实店铺测试期间一定要关掉，否则会打扰真实买家
        fulfillment.put("notify_customer", false);

        // 最外层
        Map<String, Object> body = new HashMap<>();
        body.put("fulfillment", fulfillment);

        // POST 发出去。响应体我们不关心（成功了就行），用 String 接一下
        restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class
        );
    }
}
