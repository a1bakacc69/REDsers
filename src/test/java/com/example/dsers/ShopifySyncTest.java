package com.example.dsers;

import com.example.dsers.client.ShopifyClient;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 真实对接测试：主动拉取 Shopify 订单 → 存进数据库
 */
@SpringBootTest
class ShopifySyncTest {

    @Autowired
    private ShopifyClient shopifyClient;

    @Autowired
    private OrderService orderService;

    @Test
    void testFetchAndSave() {
        // 1. 拉取真实订单
        List<ShopifyOrderDto> orders = shopifyClient.fetchOrders();
        System.out.println("拉到订单数：" + orders.size());

        // 2. 遍历每个订单，存库
        for (ShopifyOrderDto order : orders) {
            orderService.saveOrder(order);
            System.out.println("已保存订单号：" + order.getOrderNumber());
        }

        System.out.println("全部订单同步完成！");
    }
}
