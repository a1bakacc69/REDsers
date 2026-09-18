package com.example.dsers.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final Integer code;   // 错误码，R 要用它

    public BizException(Integer code, String msg) {
        super(msg);               // ← 把 msg 存进父类，getMessage() 才有值
        this.code = code;
    }
}
