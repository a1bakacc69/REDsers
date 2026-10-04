package com.example.dsers.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dsers.client.ShopifyClient;
import com.example.dsers.client.SupplierClient;
import com.example.dsers.common.BizException;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.dto.SupplierOrderQueryResponse;
import com.example.dsers.dto.SupplierOrderRequest;
import com.example.dsers.entity.OrderItem;
import com.example.dsers.entity.PlatformOrder;
import com.example.dsers.enums.OrderStatus;
import com.example.dsers.enums.SaveResult;
import com.example.dsers.mapper.OrderItemMapper;
import com.example.dsers.mapper.PlatformOrderMapper;
import com.example.dsers.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class OrderService {

    @Autowired
    private PlatformOrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ShopifyClient shopifyClient;

    @Autowired
    private OrderSyncService orderSyncService;

    @Autowired
    private SupplierClient supplierClient;

    // 去 Shopify 拉全量，逐条同步进库；返回一句话统计
    public String pullOrders(){
        List<ShopifyOrderDto> ordersdto = shopifyClient.fetchOrders();

        int inserted = 0;
        int updated = 0;
        for (ShopifyOrderDto order : ordersdto) {
            if (orderSyncService.saveOrder(order) == SaveResult.INSERTED) {
                inserted++;
            } else {
                updated++;
            }
        }
        return "共拉到订单数：" + ordersdto.size()
                + "\n新增：" + inserted + " 条"
                + "\n更新：" + updated + " 条";
    }

    public List<OrderVO> listOrders(){
        List<PlatformOrder> orders = orderMapper.selectList(null);

        List<OrderVO> list = new ArrayList<>();

        for(PlatformOrder order : orders) {
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

    public OrderVO getOrderById(Long id) {
        PlatformOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order,vo);
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId,vo.getId());
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        vo.setItems(items);
        return vo;
    }

    public String placeOrder(Long orderId){

        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 幂等，防止重复点击
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，不能重复下单");
        }
        // 空订单不给下单
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            throw new BizException(400, "订单没有商品明细，无法下单");
        }

        SupplierOrderRequest req = new SupplierOrderRequest();
        req.setClientOrderNo(order.getId());        // 幂等键 = orders.id
        req.setReceiverName(order.getShippingName());
        req.setReceiverPhone(order.getShippingPhone());
        req.setReceiverAddress1(order.getShippingAddress1());
        req.setReceiverCity(order.getShippingCity());
        req.setReceiverZip(order.getShippingZip());
        req.setReceiverProvince(order.getShippingProvince());
        req.setReceiverCountryCode(order.getShippingCountryCode());
        // 明细抽成单独方法
        req.setItems(toSupplierItems(items));

        String supplierOrderId = supplierClient.placeOrder(req);

        orderMapper.updatePlaced(orderId, supplierOrderId, OrderStatus.ORDERED.name());

        return "下单成功，供货商单号：" + supplierOrderId;

    }

    private List<SupplierOrderRequest.Item> toSupplierItems(List<OrderItem> items) {
        List<SupplierOrderRequest.Item> list = new ArrayList<>();
        for (OrderItem item : items) {
            SupplierOrderRequest.Item si = new SupplierOrderRequest.Item();
            si.setTitle(item.getTitle());
            si.setQuantity(item.getQuantity());
            si.setPrice(item.getPrice());
            si.setVariantId(item.getVariantId());
            list.add(si);
        }
        return list;
    }

    // 去供货商那儿问发货没，发了就把运单号写回来
    @Transactional
    public String refreshShipping(Long orderId) {

        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        if (!OrderStatus.ORDERED.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，无需查询物流");
        }

        if (order.getSupplierOrderId() == null) {
            throw new BizException(400, "订单还没有供货商单号，无法查询物流");
        }

        SupplierOrderQueryResponse.Data data = supplierClient.queryOrder(order.getSupplierOrderId());

        // 还没发货不是错误，正常返回
        if (!OrderStatus.SHIPPED.name().equals(data.getStatus())) {
            return "供货商尚未发货（当前状态：" + data.getStatus() + "）";
        }

        orderMapper.updateShipped(
                orderId,
                data.getTrackingNumber(),
                data.getTrackingCompany(),
                OrderStatus.SHIPPED.name()
        );

        return "已获取运单号：" + data.getTrackingNumber() + "（" + data.getTrackingCompany() + "）";
    }

    // 回写运单号给 Shopify，闭环最后一步。故意不加 @Transactional：
    // 只有最后一次写库，中间夹着两次 HTTP，包事务反而占着数据库连接。
    // 另外远程成功、本地写库失败这种「一半成功」，事务也救不了，得靠补偿。
    public String pushTracking(Long orderId) {

        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，无需回写");
        }

        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            throw new BizException(400, "订单还没有运单号，无法回写平台");
        }

        Long fulfillmentOrderId = shopifyClient.fetchFulfillmentOrderId(order.getPlatformOrderId());
        if (fulfillmentOrderId == null) {
            throw new BizException(500, "Shopify 未返回 fulfillment_order_id，无法回写");
        }

        shopifyClient.createFulfillment(
                fulfillmentOrderId,
                order.getTrackingNumber(),
                order.getTrackingCompany()
        );

        orderMapper.updateSynced(orderId, OrderStatus.SYNCED.name());

        return "已回写平台：运单号 " + order.getTrackingNumber()
                + "（" + order.getTrackingCompany() + "）";
    }

}
