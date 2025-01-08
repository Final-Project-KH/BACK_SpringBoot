package com.kh.totalproject.service;

import com.kh.totalproject.constant.UserStatus;
import com.kh.totalproject.dto.response.GoogleUserInfoResponse;
import com.kh.totalproject.dto.response.TokenResponse;
import com.kh.totalproject.entity.User;
import com.kh.totalproject.repository.UserRepository;
import com.kh.totalproject.util.JwtUtil;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class GoogleService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public TokenResponse loginWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            log.error("구글 ID 토큰이 유효하지 않습니다.");
            throw new IllegalArgumentException("구글 ID 토큰이 유효하지 않습니다.");
        }

        // ID Token 검증 및 사용자 정보 추출
        log.info("구글 ID 토큰 검증 시작");
        OAuth2User user = validateAndExtractUserInfo(idToken);

        if (user == null) {
            log.error("구글 사용자 정보 가져오기 실패");
            throw new IllegalArgumentException("구글 사용자 정보 가져오기 실패");
        }

        // 이메일로 기존 사용자 검색, 없으면 새 사용자 생성
        log.info("이메일 {} 로 기존 사용자 검색", Optional.ofNullable(user.getAttribute("email")));
        User member = userRepository.findByEmail(user.getAttribute("email"))
                .orElseGet(() -> {
                    log.info("기존 사용자가 없으므로 새 사용자 생성");
                    return createNewMember(user);
                });

        // AuthenticationToken 생성 및 인증 처리
        log.info("구글 인증 처리 시작");
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                user, Collections.singleton(new SimpleGrantedAuthority("USER")), "email"
        );

        // 인증 처리
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // JWT 토큰 생성
        log.info("JWT 토큰 생성 완료");
        return jwtUtil.generateToken(authentication);
    }

    private OAuth2User validateAndExtractUserInfo(String idToken) {
        try {
            // ID Token을 파싱하여 클레임 추출
            log.info("구글 ID 토큰 파싱 시작");
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");

            if (email == null || name == null) {
                log.error("ID 토큰에 이메일 또는 이름 정보가 없습니다.");
                throw new IllegalArgumentException("ID 토큰에서 사용자 정보를 추출할 수 없습니다.");
            }

            log.info("구글 사용자 정보: 이메일={}, 이름={}", email, name);
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("USER")),
                    Map.of("email", email, "name", name),
                    "email"
            );
        } catch (ParseException e) {
            log.error("ID 토큰 파싱 실패: 메시지={}", e.getMessage());
            throw new IllegalArgumentException("구글 ID 토큰 검증 실패");
        }
    }

    private User createNewMember(OAuth2User user) {
        log.info("새 사용자 생성 시작");
        User member = new User();
        String email = user.getAttribute("email");

        // Extract the username part (before @)
        String usernamePart = email.split("@")[0];

        // Extract the domain part (between @ and .com)
        String domainPart = email.split("@")[1].split("\\.")[0];

        // Combine to create userId
        String userId = usernamePart + domainPart;

        member.setEmail(email);
        member.setUserId(userId); // Set userId as combination of username and domain part
        member.setNickname("User_" + UUID.randomUUID().toString().substring(0, 8));
        member.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // 임시 비밀번호 생성
        member.setUserStatus(UserStatus.USER);

        log.info("새 사용자 정보 저장: 사용자 ID = {}, 이메일 = {}", userId, email);
        return userRepository.save(member);
    }
}
