package com.tenco.class_jwt_vo1.service;

import com.tenco.class_jwt_vo1.domain.User;
import com.tenco.class_jwt_vo1.dto.LoginResponseDto;
import com.tenco.class_jwt_vo1.dto.RegisterRequestDto;
import com.tenco.class_jwt_vo1.mapper.UserMapper;
import com.tenco.class_jwt_vo1.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    // DB 조회, 인설트, 업데이트
    @Autowired // DI 처리
    private UserMapper userMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Transactional
    public void register(RegisterRequestDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        userMapper.save(user);
    }
    public LoginResponseDto login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // DB 이름 있고 , 비밀번호가 맞다면 -- JWT 토큰 발행해 주면 된다.
            String accessToken = jwtUtil.generateAccessToken(username);
            String refreshToken = jwtUtil.generateRefreshToken(username);

            // 로그인을 했다면 리프레쉬 토큰 업데이트 쿼리 실행
            user.setRefreshToken(refreshToken);

            userMapper.updateRefreshToken(user); // update
            return new LoginResponseDto(accessToken, refreshToken, username);
        }
        // 인증 실패 했다면
        return null;
    }
    // 새로운 액세스 토큰을 생성한다 (리프레쉬 토큰이 있어야한다)
    public String refreshAccessToken(String refreshToken) {
        // 리프레쉬 토큰에서 사용자 이름을 추출함.
        String username = jwtUtil.extractUsername(refreshToken);
        // DB 조회 ==
        // 사용자가 가지고있는 -refreshToken
        // DB에 저장된(7일) -refreshToken
        User user = userMapper.findByUsername(username);
        // 사용자 존재 OK (DB) && 토큰 검증(유효기간 확인)
        if(user != null
                && jwtUtil.validateToken(refreshToken, username)
                && refreshToken.equals(user.getRefreshToken())) {
                // 문제가 없다면 다시 액세스 토큰을 발급할 수 있다.
                return jwtUtil.generateRefreshToken(username);
        }
        // 조건 불만족 시 null 반환(갱신 실패)
        return null;
    }
    // JWT 동작 흐름 이해
    // 로그아웃을 구현하더라도 (refreshToken 을 무효화 )
    // 로그아웃 하더라도 10분간은 인증이 필요한 페이지에 접근이 가능하다.
    // 해결방안
    // 1. 로그아웃 시 accessToken 서버 블랙리스트(예 Redis)에 저장
    //      validateToken() -- (RedisTemplate 활용해서 블랙 리스트에 저장된 토큰이라면 무효화 처리)
    // 2. 클라이언트 협력(로컬에 저장된 토큰을 삭제 --> 서버는 신경 안 써도 됨)
    // 3. 실무에서 선택 방안
    //      방법 : accessToken 유효 시간을 짧게 (예 1분) 설정 --> 만료 후 바로
    //      자주 갱신을 함 --> 사용자 경험 저하
    //      일반적으로 : 블랙리스트 + 적당히 짧은 만료시간을 조합해서 사용

    public void logout(String refreshToken) {
       String username = jwtUtil.extractUsername(refreshToken);
       User user = userMapper.findByUsername(username);
       if (user != null) {
           user.setRefreshToken(null);
           userMapper.updateRefreshToken(user);
       }
    }
}
