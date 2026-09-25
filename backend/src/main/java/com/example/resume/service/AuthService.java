package com.example.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.resume.common.BusinessException;
import com.example.resume.dto.LoginRequest;
import com.example.resume.entity.User;
import com.example.resume.mapper.UserMapper;
import com.example.resume.security.JwtUtil;
import com.example.resume.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginVO(token, user.getUsername(), user.getNickname(), user.getAvatar());
    }
}
