package com.example.dsers.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dsers.entity.PlatformOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlatformOrderMapper extends BaseMapper<PlatformOrder> {

    /**
     * 平台数据覆盖。SET 里只列平台侧字段，status 等业务字段没有入口，不会被刷掉。
     *
     * 没用 updateById 是因为它跳过 null 字段，而 Shopify 返回的是全量快照 ——
     * 买家把地址第二行清空后接口返回 null，跳过 null 就刷不掉，库里会一直留着旧地址。
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

    /** 只推状态。运单号上一步已经落库了，这里不给它入口 */
    @Update("""
        UPDATE orders
           SET status = #{status}
         WHERE id = #{id}
        """)
    int updateSynced(@Param("id") Long id, @Param("status") String status);

}
