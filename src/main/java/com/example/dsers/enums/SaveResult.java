package com.example.dsers.enums;

import lombok.Getter;

/**
 * 一次订单同步的结果。
 * 没用 boolean：同步有「新增 / 刷新 / 平台已存在」几种结局，塞进 boolean 会让
 * 「已存在」读成失败，但它其实是幂等操作的正常结果，所以用真实的名字说清楚。
 */
@Getter
public enum SaveResult {

    /** 库里没有 → 本次新增（连明细一起插） */
    INSERTED("新增"),

    /** 库里已有 → 只刷新 A 区（平台镜像）字段，明细不动 */
    UPDATED("更新");

    private final String label;

    SaveResult(String label) {
        this.label = label;
    }
}
