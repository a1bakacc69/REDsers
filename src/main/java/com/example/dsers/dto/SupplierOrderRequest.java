package com.example.dsers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发给供货商平台的下单请求。
 * 字段与供货商侧的 PlaceOrderRequest 一一对应（契约由对方定义，我们照抄）。
 */
@Data
public class SupplierOrderRequest {

    /** 幂等键 = orders.id */
    private Long clientOrderNo;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress1;
    private String receiverCity;
    private String receiverZip;
    private String receiverProvince;
    private String receiverCountryCode;

    private List<Item> items;

    /** 一行商品。嵌套类：因为它只在「请求」里出现，没必要单独一个文件 */
    @Data
    public static class Item {
        private String title;
        private Integer quantity;
        private BigDecimal price;
        private Long variantId;
    }
}
