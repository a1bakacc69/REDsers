package com.example.dsers.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_items")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String title;

    // ==================== 阶段2新增：商品标识（认规格 + 回写用）====================

    /** 平台侧明细行ID。★ 回写发货时用它说明"发的是哪几行" */
    private Long platformLineItemId;

    /** 平台侧变体ID（具体规格）。比 sku 可靠 —— sku 实测可能为 null */
    private Long variantId;

    /** 平台侧商品ID */
    private Long productId;

    /** 规格名称，如 150cm / Default Title */
    private String variantTitle;

    private Integer quantity;

    private BigDecimal price;

    private String sku;
}
