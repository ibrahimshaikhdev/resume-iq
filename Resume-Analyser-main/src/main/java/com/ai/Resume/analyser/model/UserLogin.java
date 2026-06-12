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

    @Size(min = 6, max = 20, message = "password must be between 6 and 20 characters")
    private String password;
}
