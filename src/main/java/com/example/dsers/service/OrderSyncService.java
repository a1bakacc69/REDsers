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

    /** 当前接入的平台标识（接入第二个平台时再抽成枚举/常量类） */
    private static final String PLATFORM_SHOPIFY = "SHOPIFY";

    @Autowired
    private PlatformOrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    /** 转换器：负责 DTO → 实体，Service 不再自己拼装字段 */
    @Autowired
    private ShopifyOrderConverter shopifyOrderConverter;

    /**
     * 保存一条平台订单：<b>库里没有就新增，已经有了就只刷新 A 区</b>。
     *
     * <p>这是「同步」这个动作的完整含义 —— 同步不是「只进不出」，
     * 平台那边改了地址、取消了订单，都要能跟过来。
     *
     * @return 本次到底是新增还是更新，由调用方（Controller/定时任务）决定怎么表述
     */
    @Transactional
    public SaveResult saveOrder(ShopifyOrderDto dto) {

        // 幂等第一层：先查后插
        // 注意查询条件要和唯一索引 uk_platform_order(platform, platform_order_id) 保持一致
        LambdaQueryWrapper<PlatformOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformOrder::getPlatform, PLATFORM_SHOPIFY)
                .eq(PlatformOrder::getPlatformOrderId, dto.getId());
        PlatformOrder existing = orderMapper.selectOne(wrapper);

        // 1. 转换：DTO → 实体（全部交给 Converter）
        //    ★ 新增和更新共用这一份 —— 因为 toEntity 只映射 A 区字段，
        //      它本身就是「A 区字段有哪些」的唯一清单，不需要再写第二份
        PlatformOrder fresh = shopifyOrderConverter.toEntity(dto);

        // ==================== 情况一：库里没有 → 新增 ====================
        if (existing == null) {
            // 2. 插入主表，拿到回填的 id
            orderMapper.insert(fresh);
            Long orderId = fresh.getId();

            // 3. 插入每个明细
            for (OrderItemDTO orderItemDTO : dto.getLineItems()) {
                orderItemMapper.insert(shopifyOrderConverter.toItemEntity(orderItemDTO, orderId));
            }
            return SaveResult.INSERTED;
        }

        // ==================== 情况二：库里已有 → 只刷新 A 区 ====================
        // 注意 fresh 里此刻还带着 toEntity 赋的 status='PENDING'（常量赋值那一步），
        // 但 updateZoneA 的 SQL 里没有 status 这一列 —— 这个值会被数据库直接忽略。
        // 这就是 B 区的保险：不是「记得别碰」，是「想碰都没有通道」。
        fresh.setId(existing.getId());      // 只补一个主键，其余全是刚拉到的 A 区数据
        orderMapper.updateZoneA(fresh);
        return SaveResult.UPDATED;
    }
}
