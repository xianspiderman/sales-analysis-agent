package com.dyh.salesAgent.controller;
import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice // 全局异常处理注解
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse("参数校验失败");
        return ResponseEntity.badRequest().body(error("INVALID_ARGUMENT", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class) // 映射400， 表示调用者输入不合法
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(error("INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Map<String, String>> handleNotLogin(NotLoginException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("NOT_LOGIN", "请先登录后再访问"));
    }

    @ExceptionHandler(Exception.class) // 500兜底
    public ResponseEntity<Map<String, String>> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.internalServerError()
                .body(error("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
    }

    private Map<String, String> error(String code, String message) {
        return Map.of("code", code, "message", message == null ? "参数校验失败" : message);
    }
}
