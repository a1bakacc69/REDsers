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

    /**
     * 拉取订单：去 Shopify 拉全量，逐条同步进库。
     *
     * @return 给前端/日志看的一句话统计
     */
    public String pullOrders(){
        // 1. 拉取真实订单
        List<ShopifyOrderDto> ordersdto = shopifyClient.fetchOrders();

        // 2. 遍历每个订单，存库
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

    //订单列表
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

    //订单详细
    public OrderVO getOrderById(Long id) {
        // 【1】按 id 查主表 —— 用 selectById
        PlatformOrder order = orderMapper.selectById(id);   // 按主键查一条，查不到返回 null
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 【2】new VO，把订单字段拷进去
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order,vo);
        // 【3】按 order_id 查明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId,vo.getId());
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        // 【4】装进 VO
        vo.setItems(items);
        // 【5】返回
        return vo;
    }

    //提交订单给供货商
    public String placeOrder(Long orderId){

        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 幂等，防止重复点击
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，不能重复下单");
        }
        //查一下明细，如果是空订单就不下
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            throw new BizException(400, "订单没有商品明细，无法下单");
        }

        //开始拼要给供货商的对象
        SupplierOrderRequest req = new SupplierOrderRequest();
        req.setClientOrderNo(order.getId());        // ★ 幂等键 = orders.id
        req.setReceiverName(order.getShippingName());
        req.setReceiverPhone(order.getShippingPhone());
        req.setReceiverAddress1(order.getShippingAddress1());
        req.setReceiverCity(order.getShippingCity());
        req.setReceiverZip(order.getShippingZip());
        req.setReceiverProvince(order.getShippingProvince());
        req.setReceiverCountryCode(order.getShippingCountryCode());
        //填充订单明细的抽个新方法
        req.setItems(toSupplierItems(items));

        //拿到回填订单id
        String supplierOrderId = supplierClient.placeOrder(req);

        //回写数据库
        orderMapper.updatePlaced(orderId, supplierOrderId, OrderStatus.ORDERED.name());

        return "下单成功，供货商单号：" + supplierOrderId;

    }

    //placeorder需要用到的，用于组装订单明细
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

    /**
     * 刷新物流：去供货商那儿问一句「发货了吗」，发了就把运单号写回来。
     */
    @Transactional
    public String refreshShipping(Long orderId) {

        // 【1】查订单
        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 【2】只有「已下单未发货」的才需要查物流
        if (!OrderStatus.ORDERED.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，无需查询物流");
        }

        // 【3】没有供货商单号就无从查起
        if (order.getSupplierOrderId() == null) {
            throw new BizException(400, "订单还没有供货商单号，无法查询物流");
        }

        // 【4】问供货商
        SupplierOrderQueryResponse.Data data = supplierClient.queryOrder(order.getSupplierOrderId());

        // 【5】对方还没发货 —— 这不是错误，是正常情况
        if (!OrderStatus.SHIPPED.name().equals(data.getStatus())) {
            return "供货商尚未发货（当前状态：" + data.getStatus() + "）";
        }

        // 【6】发了 → 把运单号写回来
        orderMapper.updateShipped(
                orderId,
                data.getTrackingNumber(),
                data.getTrackingCompany(),
                OrderStatus.SHIPPED.name()
        );

        return "已获取运单号：" + data.getTrackingNumber() + "（" + data.getTrackingCompany() + "）";
    }

    /**
     * 回写平台：把我们手里的运单号交给 Shopify，让买家在平台上能看到物流。
     *
     * <p>这是闭环的最后一步。和 refreshShipping 是对称的：
     * <pre>
     *   refreshShipping：供货商 ──运单号──▶ 我们
     *   pushTracking   ：我们 ──运单号──▶ Shopify
     * </pre>
     *
     * <p>Shopify 的回写要两步：先拿 fulfillment_order_id，再用它提交履约。
     * 这两步都封装在 ShopifyClient 里了，这里只负责编排 + 改状态。
     *
     * <p><b>这里没有加 @Transactional</b>，故意的：
     * 整个方法只有一次写库（最后的 updateSynced），事务没有意义；
     * 而且中间夹着两次 HTTP 调用，真包了事务反而会让数据库连接被占住。
     *
     * <p>更要紧的是：远程调用成功、本地写库失败，这种「一半成功」的处境
     * 本质上要靠分布式事务或对账补偿来解决，一个 @Transactional 救不了。
     * 本项目不展开，但知道有这回事就够了。
     */
    public String pushTracking(Long orderId) {

        // 【1】查订单
        PlatformOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 【2】只有「已发货待回写」的才需要推给平台
        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BizException(400, "订单当前状态为「" + order.getStatus() + "」，无需回写");
        }

        // 【3】没有运单号推什么？
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            throw new BizException(400, "订单还没有运单号，无法回写平台");
        }

        // 【4】第一步：问 Shopify 要履约单 id
        Long fulfillmentOrderId = shopifyClient.fetchFulfillmentOrderId(order.getPlatformOrderId());
        if (fulfillmentOrderId == null) {
            throw new BizException(500, "Shopify 未返回 fulfillment_order_id，无法回写");
        }

        // 【5】第二步：带上运单号，提交履约
        shopifyClient.createFulfillment(
                fulfillmentOrderId,
                order.getTrackingNumber(),
                order.getTrackingCompany()
        );

        // 【6】平台那边成功了，本地状态推到终点
        orderMapper.updateSynced(orderId, OrderStatus.SYNCED.name());

        return "已回写平台：运单号 " + order.getTrackingNumber()
                + "（" + order.getTrackingCompany() + "）";
    }

}
