package com.example.dsers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderItemDTO {

    /** 平台侧明细行ID（Shopify 的 line_items[].id）
     *  → 转换时映射到 order_items.platform_line_item_id，供回写发货使用 */
    private Long id;

    private String title;

    // ==================== 阶段2新增：商品标识 ====================

    /** 平台侧变体ID（具体规格）。比 sku 可靠 —— sku 实测可能为 null */
    @JsonProperty("variant_id")
    private Long variantId;

    /** 平台侧商品ID */
    @JsonProperty("product_id")
    private Long productId;

    /** 规格名称，如 150cm / Default Title */
    @JsonProperty("variant_title")
    private String variantTitle;

    private Integer quantity;

    /** ⚠️ Shopify 给的是字符串，入库前要转 BigDecimal */
    private String price;

    /** ⚠️ 商家自定义编码，可能为 null */
    private String sku;
}
