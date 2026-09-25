package com.example.resume.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private String username;
    private String nickname;
    private String avatar;
}
