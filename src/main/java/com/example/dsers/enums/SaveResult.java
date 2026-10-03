package com.example.dsers.enums;

import lombok.Getter;

/**
 * 一次订单同步的结果。
 *
 * <p><b>为什么不用 boolean？</b>
 * boolean 只有「真/假」两种，但同步其实有三种可能的结局：
 * 新增了一条、刷新了一条、还是平台发来的订单我们库里有但字段全是旧的。
 * 用 boolean 表达时，「已存在」被硬塞成了 false —— 而 false 读起来像「失败」。
 *
 * <p>但「已存在」不是失败，是幂等操作的<b>正常结果</b>（见《项目进度快照》4.4）。
 * 我们用真实的名字把这件事说清楚：INSERTED / UPDATED。
 */
@Getter
public enum SaveResult {

    /** 库里没有这条订单 → 本次是新增（连同明细一起插入） */
    INSERTED("新增"),

    /** 库里已有这条订单 → 本次只刷新了 A 区（平台镜像）字段，明细不动 */
    UPDATED("更新");

    private final String label;

    SaveResult(String label) {
        this.label = label;
    }
}
