package com.hxy.hxailife.travel;

import com.hxy.hxailife.controller.TravelPlanController;
import com.hxy.hxailife.controller.TravelPlanFavoriteController;
import com.hxy.hxailife.controller.TravelPlanRefineController;
import com.hxy.hxailife.controller.ChatController;
import com.hxy.hxailife.controller.LocationController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {TravelPlanController.class, TravelPlanFavoriteController.class,
        TravelPlanRefineController.class, ChatController.class, LocationController.class})
public class TravelPlanExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handlePlanException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", exception.getReason()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "请求格式不正确，请检查日期、时间、ID 和枚举取值"));
    }
}
