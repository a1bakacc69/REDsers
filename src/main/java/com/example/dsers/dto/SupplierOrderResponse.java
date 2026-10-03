package com.example.dsers.dto;

import lombok.Data;

/**
 * 供货商平台返回的响应。
 * 对应 JSON：{"code":200,"msg":"ok","data":{"supplierOrderId":"SUP-xxx"}}
 */
@Data
public class SupplierOrderResponse {

    private Integer code;
    private String msg;
    private Data data;

    @lombok.Data
    public static class Data {
        private String supplierOrderId;
    }
}
