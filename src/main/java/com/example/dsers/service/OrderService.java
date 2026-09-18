package com.example.dsers.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dsers.common.BizException;
import com.example.dsers.dto.OrderItemDTO;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.entity.OrderItem;
import com.example.dsers.entity.ShopifyOrder;
import com.example.dsers.mapper.OrderItemMapper;
import com.example.dsers.mapper.ShopifyOrderMapper;
import com.example.dsers.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class OrderService {

    @Autowired
    private ShopifyOrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    //保存订单数据至mysql
    @Transactional
    public void saveOrder(ShopifyOrderDto dto) {
        //先进行判断，是否保存过？
        LambdaQueryWrapper<ShopifyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopifyOrder::getShopifyOrderId, dto.getId());
        if (orderMapper.selectCount(wrapper) > 0) {
            return;   // 已存在，直接跳过
        }
        // 1. 把 DTO 转成主表实体 ShopifyOrder
        ShopifyOrder shopifyOrder = new ShopifyOrder();
        BeanUtils.copyProperties(dto, shopifyOrder);  // 自动把 dto 里同名字段复制到 shopifyOrder
        shopifyOrder.setId(null);
        shopifyOrder.setTotalPrice(
                dto.getTotalPrice() != null ? new BigDecimal(dto.getTotalPrice()) : BigDecimal.ZERO
        );

        // 第一步：解析成带时区的时间
        OffsetDateTime odt = OffsetDateTime.parse(dto.getCreatedAt());
        // 第二步：转成 LocalDateTime（去掉时区，存数据库）
        LocalDateTime dt = odt.toLocalDateTime();
        shopifyOrder.setCreatedAt(dt);
        shopifyOrder.setShopifyOrderId(dto.getId());
        // 2. 插入主表，拿到回填的 id
        orderMapper.insert(shopifyOrder);
        Long orderId = shopifyOrder.getId();
        // 3. 遍历 dto.getLineItems()，每个转成 OrderItem，orderId 设为那个 id
        for (OrderItemDTO orderItemDTO : dto.getLineItems()){
            OrderItem orderItem = new OrderItem();
            BeanUtils.copyProperties(orderItemDTO, orderItem);
            orderItem.setId(null);
            orderItem.setPrice(new BigDecimal(orderItemDTO.getPrice()));
            orderItem.setOrderId(orderId);
            // 4. 插入每个明细
            orderItemMapper.insert(orderItem);
        }

    }

    //订单列表
    public List<OrderVO> listOrders(){
        List<ShopifyOrder> orders = orderMapper.selectList(null);

        List<OrderVO> list = new ArrayList<>();

        for(ShopifyOrder order : orders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderItem::getOrderId, orderVO.getId());
            List<OrderItem> items = orderItemMapper.selectList(wrapper);
            orderVO.setItems(items);
            list.add(orderVO);
        }
        return list;
    }
    //订单详细
    public OrderVO getOrderById(Long id) {
        // 【1】按 id 查主表 —— 用 selectById
        ShopifyOrder order = orderMapper.selectById(id);   // 按主键查一条，查不到返回 null
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 【2】new VO，把订单字段拷进去
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order,vo);
        // 【3】按 order_id 查明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId,vo.getId());   // ← 填什么？
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        // 【4】装进 VO
        vo.setItems(items);
        // 【5】返回
        return vo;
    }

}
