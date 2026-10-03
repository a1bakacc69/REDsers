package com.example.dsers.dto;

import lombok.Data;

/**
 * 供货商「查询发货状态」接口的响应。
 * 对应 JSON：{"code":200,"msg":"ok","data":{"supplierOrderId":"...","status":"SHIPPED",
 *            "trackingNumber":"231669096273","trackingCompany":"DHL"}}
 */
@Data
public class SupplierOrderQueryResponse {

    private Integer code;
    private String msg;
    private Data data;

    @lombok.Data
    public static class Data {
        private String supplierOrderId;
        private String status;            // RECEIVED / SHIPPED
        private String trackingNumber;    // 发货前是 null
        private String trackingCompany;
    }
}
