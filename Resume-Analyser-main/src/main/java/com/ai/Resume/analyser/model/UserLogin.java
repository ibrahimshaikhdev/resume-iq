package com.ai.Resume.analyser.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLogin {

    @Email
    @NotBlank(message = "Email must not be empty")
    private String email;

    @Size( min = 6, max =  16 , message = "password has atLeast 6 characters and atMax 16 characters")
    private  String password;
}
