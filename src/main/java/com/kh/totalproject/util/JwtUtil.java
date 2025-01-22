package com.kh.totalproject.util;


import com.kh.totalproject.config.JwtConfig;
import com.kh.totalproject.dto.response.TokenResponse;
import com.kh.totalproject.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Component
// 기존 강의의 TokenProvider 역할
public class JwtUtil {

    private final String secretKey;
    private static final long EXPIRATION_TIME = 1000*60*60;
    private final UserRepository userRepository;

    // Secret key 받아오기
    public JwtUtil(JwtConfig jwtConfig, UserRepository userRepository){
        this.secretKey= jwtConfig.getSecretKey();
        this.userRepository = userRepository;
    }

    // JWT 생성
//    public TokenResponse generateToken(Authentication authentication){
//        // Login 시 인증 권한 요청한 아이디 authentication 기반으로 CustomUserDetails 생성
//        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
//        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
//        long now = (new Date()).getTime();
//        Date accessTokenExpiresIn = new Date(now + EXPIRATION_TIME);
//        Date refreshTokenExpiresIn = new Date(now + 60 * 60 * 1000 * 24 * 6); // 6일
//
//        // Access Token 생성
//        // 만기 시간 : 1시간
//        String accessToken =  Jwts.builder()
//                .subject(String.valueOf(userDetails.getId())) // sub : Long id (이 정보를 기반으로 앞으로 관련 정보 탐색)
//                .claim("nickname", userDetails.getNickname()) // claim : String nickname
//                .claim("authorities", userDetails.getAuthorities()) // claim : 권한 (User Or Admin)
//                .issuedAt(new Date())
//                .expiration(accessTokenExpiresIn)
//                .signWith(key, SignatureAlgorithm.HS256) // 우선 HS256으로 적용
//                .compact();
//
//        // Refresh Token 생성
//        // 만기 시간 : 6일
//        String refreshToken = Jwts.builder()
//                .subject(String.valueOf(userDetails.getId()))
//                .claim("nickname", userDetails.getNickname())
//                .claim("authorities", userDetails.getAuthorities())
//                .issuedAt(new Date())
//                .expiration(refreshTokenExpiresIn)
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//
//        // 설정한 TokenResponse 형태에 맞춰서 변환
//        return TokenResponse.builder()
//                .grantType("Bearer")
//                .accessToken(accessToken)
//                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
//                .refreshToken(refreshToken)
//                .refreshTokenExpiresIn(refreshTokenExpiresIn.getTime())
//                .build();
//    }

    // 기존 generateToken(Authentication) 메서드 유지
    public TokenResponse generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateTokenFromUserDetails(userDetails);
    }

    // 새로운 메서드: User 엔티티 기반 JWT 생성
    public TokenResponse generateTokenFromUser(com.kh.totalproject.entity.User user) {
        // CustomUserDetails 생성 없이 바로 JWT 발급을 위해 필요한 정보 추출
        // 필요한 경우, CustomUserDetails를 생성할 수도 있습니다.
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserKey(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString()))
        );
        return generateTokenFromUserDetails(userDetails);
    }

    // 내부적으로 JWT 생성 로직을 담당하는 메서드
    private TokenResponse generateTokenFromUserDetails(CustomUserDetails userDetails) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + EXPIRATION_TIME);
        Date refreshTokenExpiresIn = new Date(now + 60 * 60 * 1000 * 24 * 6); // 6일

        // Access Token 생성
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claim("nickname", userDetails.getNickname())
                .claim("authorities", userDetails.getAuthorities())
                .issuedAt(new Date())
                .expiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claim("nickname", userDetails.getNickname())
                .claim("authorities", userDetails.getAuthorities())
                .issuedAt(new Date())
                .expiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String token){
        Claims claims = parseToken(token);

        String primaryKey = claims.getSubject();
        com.kh.totalproject.entity.User user = userRepository.findById(Long.valueOf(primaryKey))
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        Collection<? extends GrantedAuthority> authorities=
                Arrays.stream(claims.get("authorities").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        // Access Token 만료시 인증 객체 커스텀 필드
        // 로그인때와 마찬가지로 유저 정보를 확인후 검증
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getEmail(),
                user.getNickname(),
                user.getUserId(), // primaryKey
                user.getUserKey(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().toString()))
        );
        return new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
    }

    public Claims parseToken(String token){
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Access 토큰이 들어 왔을때 id만 따로 추출
    public Long extractUserId(String token) {
        Claims claims = parseToken(token);
        String id = claims.getSubject();
        return Long.valueOf(id);
    }

    // access 토큰 재발급
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication).getAccessToken();
    }

    public boolean validateToken(String token){
        try{
            parseToken(token);
            log.info("JWT 검증 성공"); // Debug 용
            return true;
        }
        catch (JwtException | IllegalArgumentException e){
            log.info("JWT 검증 실패 : {}", e.getMessage()); // Debug 용
            return false;
        }
    }

}
