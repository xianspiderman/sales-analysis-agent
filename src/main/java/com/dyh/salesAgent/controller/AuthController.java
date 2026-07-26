package com.dyh.salesAgent.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.dyh.salesAgent.entity.SalesRep;
import com.dyh.salesAgent.repository.SalesRepRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SalesRepRepository repRepository;
    private final PasswordEncoder passwordEncoder;

    record LoginRequest(
            @NotBlank(message = "登录账号不能为空") @Size(max = 50) String loginName,
            @NotBlank(message = "密码不能为空") @Size(max = 100) String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        SalesRep rep = repRepository.findByLoginName(request.loginName().trim()).orElse(null);
        if (rep == null || !passwordEncoder.matches(request.password(), rep.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "INVALID_CREDENTIALS", "message", "账号或密码错误"));
        }

        StpUtil.login(rep.getId());

        // 把用户信息写入 Sa-Token Session，后续请求从 Session 读取
        // 这里必须set后面的WebMvcConfig.java的preHandle才能session.get
        StpUtil.getSession()
                .set("username", rep.getName())
                .set("role",     rep.getRole())
                .set("regionId", rep.getRegionId())
                .set("repId",    rep.getId());

        return ResponseEntity.ok(Map.of(
                "token",    StpUtil.getTokenValue(),
                "username", rep.getName(),
                "role",     rep.getRole(),
                "repId",    rep.getId(),
                "regionId", rep.getRegionId()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }
}
