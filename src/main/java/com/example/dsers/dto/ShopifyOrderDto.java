package com.example.dsers.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ShopifyOrderDto {
    private Long id;
    @JsonProperty("order_number")
    private String orderNumber;
    private String name;
    private String email;
    @JsonProperty("total_price")
    private String totalPrice;
    @JsonProperty("subtotal_price")
    private String subtotalPrice;
    @JsonProperty("total_tax")
    private String totalTax;
    private String currency;
    @JsonProperty("financial_status")
    private String financialStatus;
    @JsonProperty("fulfillment_status")
    private String fulfillmentStatus;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("cancelled_at")
    private String cancelledAt;
    // 地址，下单给供应商用

    @JsonProperty("shipping_address")
    private AddressDto shippingAddress;

    /** 账单地址，仅用于姓名兜底（发货地址的 name 可能是空串） */
    @JsonProperty("billing_address")
    private AddressDto billingAddress;

    @JsonProperty("line_items")
    private List<OrderItemDTO> lineItems;
}


