package com.example.dsers.Controller;

import com.example.dsers.common.R;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.enums.SaveResult;
import com.example.dsers.service.OrderService;
import com.example.dsers.service.OrderSyncService;
import com.example.dsers.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSyncService orderSyncService;

    @PostMapping("/sync")
    public R<String> syncOrder(@RequestBody ShopifyOrderDto dto) {
        SaveResult result = orderSyncService.saveOrder(dto);
        return R.ok(result == SaveResult.INSERTED ? "同步成功，新增 1 条" : "订单已存在，已刷新平台数据");
    }
    @PostMapping("/pull")
    public R<String> pullOrders() {
        return R.ok(orderService.pullOrders());
    }

    @GetMapping("/list")                                  // ① 老张访问的地址
    public R<List<OrderVO>> listOrders() {                   // ③ 返回值 = 给前端的 JSON
        return R.ok(orderService.listOrders());                 // ④ 去取数据
    }                                                     // ② 没有参数——GET 没有请求体
    @GetMapping("/{id}")
    public R<OrderVO> getOrderById(@PathVariable Long id) {
        return R.ok(orderService.getOrderById(id));
    }

    //向供应商下单
    @PostMapping("/{id}/place")
    public R<String> placeOrder(@PathVariable Long id) {
        return R.ok(orderService.placeOrder(id));
    }

    //查询供应商那边的物流单号
    @PostMapping("/{id}/refresh-shipping")
    public R<String> refreshShipping(@PathVariable Long id) {
        return R.ok(orderService.refreshShipping(id));
    }

    //把运单号回写给 Shopify 平台
    @PostMapping("/{id}/push-tracking")
    public R<String> pushTracking(@PathVariable Long id) {
        return R.ok(orderService.pushTracking(id));
    }


}

