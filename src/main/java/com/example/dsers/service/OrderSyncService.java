package com.example.dsers.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dsers.converter.ShopifyOrderConverter;
import com.example.dsers.dto.OrderItemDTO;
import com.example.dsers.dto.ShopifyOrderDto;
import com.example.dsers.entity.PlatformOrder;
import com.example.dsers.enums.SaveResult;
import com.example.dsers.mapper.OrderItemMapper;
import com.example.dsers.mapper.PlatformOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSyncService {

    // 目前只接了 Shopify，接第二个平台时再抽成枚举
    private static final String PLATFORM_SHOPIFY = "SHOPIFY";

    @Autowired
    private PlatformOrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    // DTO → 实体都交给它，Service 不自己拼字段
    @Autowired
    private ShopifyOrderConverter shopifyOrderConverter;

    // 库里没有就新增，有就只刷 A 区。同步不是「只进不出」，
    // 平台那边改了地址、取消了订单，都得跟过来。
    @Transactional
    public SaveResult saveOrder(ShopifyOrderDto dto) {

        // 先查后插，第一层幂等
        // 条件必须和唯一索引 uk_platform_order(platform, platform_order_id) 对齐
        LambdaQueryWrapper<PlatformOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformOrder::getPlatform, PLATFORM_SHOPIFY)
                .eq(PlatformOrder::getPlatformOrderId, dto.getId());
        PlatformOrder existing = orderMapper.selectOne(wrapper);

        // 新增和更新共用这份：toEntity 只映射 A 区，
        // 它本身就是字段清单，不用再写第二份
        PlatformOrder fresh = shopifyOrderConverter.toEntity(dto);

        // 新增
        if (existing == null) {
            orderMapper.insert(fresh);
            Long orderId = fresh.getId();

            for (OrderItemDTO orderItemDTO : dto.getLineItems()) {
                orderItemMapper.insert(shopifyOrderConverter.toItemEntity(orderItemDTO, orderId));
            }
            return SaveResult.INSERTED;
        }

        // 已有：只刷 A 区
        // fresh 里还带着 toEntity 赋的 status='PENDING'，但 updateZoneA 的 SQL 没这列，
        // 会被忽略。B 区的保险不是「记得别碰」，是「想碰都没有通道」。
        fresh.setId(existing.getId());      // 只补主键，其余是刚拉到的 A 区数据
        orderMapper.updateZoneA(fresh);
        return SaveResult.UPDATED;
    }
}
