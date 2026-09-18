package com.example.dsers.common;

import lombok.Getter;

@Getter
public class R<T> {

    private final Integer code;   // 状态码
    private final String msg;     // 提示信息
    private final T data;         // 数据，类型由调用方决定

    private R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，带数据 */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "ok", data);
    }

    /** 成功，不带数据 */
    public static <T> R<T> ok() {
        return new R<>(200, "ok", null);
    }

    /** 失败 */
    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg, null);
    }
}
