package com.example.dsers.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 接住业务异常（我们主动抛的） */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /** 兜底的兜底：接住所有其他异常（我们没预料到的） */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        e.printStackTrace();                        // 真实错误打到控制台，方便你自己排查
        return R.error(500, "服务器内部错误");        // 给前端的只说这么一句
    }
}
