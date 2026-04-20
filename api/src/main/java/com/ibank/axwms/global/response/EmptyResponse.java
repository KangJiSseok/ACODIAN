package com.ibank.axwms.global.response;

/**
 * 본문이 없는 응답을 표현하는 빈 record.
 * data 가 null 이면 안 되는 ApiResponse 불변식을 깨지 않기 위한 placeholder 로 사용한다.
 * 모든 호출이 동일한 객체를 공유하도록 INSTANCE 싱글톤을 노출한다.
 */
public record EmptyResponse() {

    public static final EmptyResponse INSTANCE = new EmptyResponse();
}
