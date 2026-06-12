package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.jwt.JwtService;
import com.ai.Resume.analyser.model.User;
import com.ai.Resume.analyser.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> userdata = oAuth2User.getAttributes();

        String email = userdata.get("email").toString();
        String name = userdata.get("name").toString();

        if (!userRepository.existsById(email)) {
            User newUser = User.builder()
                    .username(name)
                    .email(email)
                    .password("")
                    .resetExpiration(null)
                    .previousResults(false)
                    .resetOtp(null)
                    .build();
            userRepository.save(newUser);
        }

        String token = jwtService.generateToken(email);
        ResponseCookie cookie = ResponseCookie.from("entrypasstoken", token).path("/").httpOnly(true).maxAge(20 * 24 * 60 * 60).sameSite("Strict").secure(false).build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.sendRedirect("/");
    }
}
