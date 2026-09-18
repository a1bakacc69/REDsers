package com.example.dsers;

import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.entity.ShopifyOrder;
import com.example.dsers.mapper.ShopifyOrderMapper;
import com.example.dsers.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
class DsersApplicationTests {

    @Autowired
    private ShopifyOrderMapper shopifyOrderMapper;

    @Autowired
    private OrderService orderService;

    @Test
    void contextLoads() {
        // 1. new 一个订单对象，手动塞数据
        ShopifyOrder order = new ShopifyOrder();
        order.setShopifyOrderId(1234567890L);
        order.setOrderNumber("1001");
        order.setName("#1001");
        order.setEmail("buyer@gmail.com");
        order.setTotalPrice(new BigDecimal("59.99"));
        order.setCurrency("USD");
        order.setFinancialStatus("paid");
        order.setCreatedAt(LocalDateTime.now());

        // 2. 插入数据库
        shopifyOrderMapper.insert(order);

        // 3. 打印插入后生成的 id（自增主键）
        System.out.println("插入成功，id = " + order.getId());

        // 4. 查出来验证
        ShopifyOrder result = shopifyOrderMapper.selectById(order.getId());
        System.out.println("查到订单号：" + result.getOrderNumber());
    }

    @Test
    void testSaveOrder() throws Exception {
        // 1. 造一份 Shopify 订单 JSON（真实格式，含时区，含两个商品明细）
        String json = """
                {
                  "id": 450789469,
                  "order_number": 1001,
                  "name": "#1001",
                  "email": "jon@doe.ca",
                  "total_price": "398.00",
                  "subtotal_price": "388.00",
                  "total_tax": "10.00",
                  "currency": "USD",
                  "financial_status": "paid",
                  "fulfillment_status": null,
                  "created_at": "2025-08-12T10:30:00-04:00",
                  "cancelled_at": null,
                  "line_items": [
                    {
                      "id": 466157049,
                      "title": "IPod Nano - 8GB",
                      "quantity": 1,
                      "price": "199.00",
                      "sku": "IPOD2008GREEN"
                    },
                    {
                      "id": 466157050,
                      "title": "IPod Nano - 16GB",
                      "quantity": 1,
                      "price": "199.00",
                      "sku": "IPOD2008SILVER"
                    }
                  ]
                }
                """;

        // 2. 用 Jackson 解析成 DTO
        ObjectMapper objectMapper = new ObjectMapper();
        ShopifyOrderDto dto = objectMapper.readValue(json, ShopifyOrderDto.class);

        System.out.println("解析成功，订单号 = " + dto.getOrderNumber() + "，商品数 = " + dto.getLineItems().size());

        // 3. 保存到数据库
        orderService.saveOrder(dto);

        System.out.println("保存成功！");
    }

}
