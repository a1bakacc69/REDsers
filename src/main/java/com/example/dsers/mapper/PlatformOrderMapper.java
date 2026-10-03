package com.example.dsers.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dsers.entity.PlatformOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlatformOrderMapper extends BaseMapper<PlatformOrder> {

    /**
     * 【A 区覆盖】把平台最新拉到的数据，刷进已有订单。
     *
     * <p><b>★ 看这个 SQL 里「有哪些列」和「没有哪些列」★</b>
     *
     * <p>出现在 SET 里的列 = <b>A 区（平台镜像）</b>，平台怎么改，我们就怎么跟。
     *
     * <p><b>故意不出现在 SET 里的列 = B 区（自有业务）</b>：
     * {@code status}、{@code supplier_order_id}、{@code tracking_number}、
     * {@code tracking_company}、{@code sync_failed}。
     * 这五列只有业务代码能写 —— 不是「我们自觉不碰」，是<b>这里根本没有入口</b>。
     *
     * <p>另外两列也不出现，各有原因：
     * <ul>
     *   <li>{@code update_time} —— C 区，表定义上有 ON UPDATE CURRENT_TIMESTAMP，数据库自己维护</li>
     *   <li>{@code platform_order_id} —— 它是订单的「身份」（用来匹配是哪一条），不是「数据」，不会变</li>
     * </ul>
     *
     * <p><b>为什么不用 {@code updateById()}？</b>
     * 它默认跳过 null 字段。但 Shopify 的 /orders.json 返回的是<b>全量快照</b>，
     * 里面出现的 null 也是事实 —— 买家把地址第二行清空了，接口就返回 {@code address2: null}，
     * 我们必须能照着把它清掉。跳过 null 的话，库里会永远留着旧地址，
     * 快递按旧地址寄出去，货就丢了。
     *
     * @param order 必须有 id；其余字段以它为准覆盖
     * @return 受影响的行数（正常是 1）
     */
    @Update("""
            UPDATE orders
               SET order_number          = #{o.orderNumber},
                   name                  = #{o.name},
                   email                 = #{o.email},
                   total_price           = #{o.totalPrice},
                   subtotal_price        = #{o.subtotalPrice},
                   total_tax             = #{o.totalTax},
                   currency              = #{o.currency},
                   shipping_name         = #{o.shippingName},
                   shipping_phone        = #{o.shippingPhone},
                   shipping_address1     = #{o.shippingAddress1},
                   shipping_address2     = #{o.shippingAddress2},
                   shipping_city         = #{o.shippingCity},
                   shipping_zip          = #{o.shippingZip},
                   shipping_province     = #{o.shippingProvince},
                   shipping_country_code = #{o.shippingCountryCode},
                   financial_status      = #{o.financialStatus},
                   fulfillment_status    = #{o.fulfillmentStatus},
                   created_at            = #{o.createdAt},
                   cancelled_at          = #{o.cancelledAt}
             WHERE id = #{o.id}
            """)
    int updateZoneA(@Param("o") PlatformOrder order);

    @Update("""
        UPDATE orders
           SET supplier_order_id = #{supplierOrderId},
               status            = #{status}
         WHERE id = #{id}
        """)
    int updatePlaced(@Param("id") Long id,
                     @Param("supplierOrderId") String supplierOrderId,
                     @Param("status") String status);

    @Update("""
        UPDATE orders
           SET tracking_number  = #{trackingNumber},
               tracking_company = #{trackingCompany},
               status           = #{status}
         WHERE id = #{id}
        """)
    int updateShipped(@Param("id") Long id,
                      @Param("trackingNumber") String trackingNumber,
                      @Param("trackingCompany") String trackingCompany,
                      @Param("status") String status);

    /**
     * 【第 4 步】回写平台成功，把状态推到终点 SYNCED。
     *
     * <p>只改 status 一列 —— 运单号在第 3 步就已经落库了，
     * 这一步不需要再动它，所以也不给它入口。
     */
    @Update("""
        UPDATE orders
           SET status = #{status}
         WHERE id = #{id}
        """)
    int updateSynced(@Param("id") Long id, @Param("status") String status);

}
