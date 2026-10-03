package com.example.dsers.enums;

import lombok.Getter;

/**
 * 订单状态机
 * 流转顺序：PENDING → ORDERED → SHIPPED → SYNCED
 */
@Getter
public enum OrderStatus {

    /** 已从平台拉取，等待处理 */
    PENDING("待处理"),

    /** 已向供应商下单 */
    ORDERED("已下单"),

    /** 供应商已发货，已拿到运单号 */
    SHIPPED("已发货"),

    /** 运单号已回写到原平台 */
    SYNCED("已同步");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }
}
