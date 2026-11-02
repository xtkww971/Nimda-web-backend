package com.nimda.cite.config;

import java.util.List;
import java.util.Objects;

/**
 * Check Configuration
 * 1. NPE Check
 * 2. Input data validation check (가능하면 Valid어노테이션으로 서블릿 필터 차원에서 해결하는게 좋음)
 *      - 서블릿 필터로 처리하지 못하는 경우(서버 자체에서 repository를 통해 데이터를 가져오거나,
 *              내부 통신을 통해 데이터를 주고받는 경우)를 처리
 */
public class CheckConfig<T>{
    public static <T> void npeCheck(T data, String fieldName){
        Objects.requireNonNull(data, () -> fieldName + " : must not be null");
    }

    public static void npeAndBlankCheck(String data, String fieldName){
        npeCheck(data, fieldName);
        if(data.isBlank()){
            throw new IllegalArgumentException(fieldName + " : must not be blank");
        }
    }

    public static void npeAndNegativeCheck(Long data, String fieldName){
        npeCheck(data, fieldName);
        if(data < 0){
            throw new IllegalArgumentException(fieldName + " : must not be negative");
        }
    }

    public static <T> void npeAndEmptyCheck(List<T> data, String fieldName){
        npeCheck(data, fieldName);
        if(data.isEmpty()){
            throw new IllegalArgumentException(fieldName + " : must not be empty");
        }
    }
}
