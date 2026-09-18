package com.example.dsers.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ShopifyOrderDto {
    private Long id;                        // 对应 JSON 的 id
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
    @JsonProperty("line_items")
    private List<OrderItemDTO> lineItems;
}


