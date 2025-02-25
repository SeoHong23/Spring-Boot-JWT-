package com.tenco.class_jwt_vo1.domain;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password; // 실제로는 암호화 필요
    // accessToken DB에 저장하지 않을 예정(10분)
    // DB 저장, 엑세스 토큰이 만료 되었을때 새 엑세스 토큰을 발급받기
    // 위해 사용(7일)
    private String refreshToken;

}
