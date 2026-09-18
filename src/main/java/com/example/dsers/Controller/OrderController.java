package com.example.dsers.Controller;

import com.example.dsers.common.R;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.service.OrderService;
import com.example.dsers.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/sync")
    public R<String> syncOrder(@RequestBody ShopifyOrderDto dto) {
        orderService.saveOrder(dto);
        return R.ok("成功");
    }
    @GetMapping("/list")                                  // ① 老张访问的地址
    public R<List<OrderVO>> listOrders() {                   // ③ 返回值 = 给前端的 JSON
        return R.ok(orderService.listOrders());                 // ④ 去取数据
    }                                                     // ② 没有参数——GET 没有请求体
    @GetMapping("/{id}")
    public R<OrderVO> getOrderById(@PathVariable Long id) {
        return R.ok(orderService.getOrderById(id));
    }


}

