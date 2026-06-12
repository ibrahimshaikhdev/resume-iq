package com.ai.Resume.analyser.jwt;

import com.ai.Resume.analyser.configuration.CustomUserDetailsService;
import com.ai.Resume.analyser.model.User;
import com.ai.Resume.analyser.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService entryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtservice;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = null;
            User user = null;
            String reqUri = request.getRequestURI();
            if (reqUri.startsWith("/resumeAnalyser/entry/v1") || reqUri.startsWith("/resumeAnalyser/public") || reqUri.startsWith("/resumeAnalyserCore/service/v1/public") || reqUri.equals("/") || reqUri.equals("/login") || reqUri.equals("/forgotpassword")) {
                filterChain.doFilter(request, response);
                return;
            }

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("entrypasstoken")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (token != null) {
                user = userRepository.findById(jwtservice.getEmail(token)).orElse(null);
            }

            if (token != null && user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtservice.validateToken(token, user.getEmail())) {
                    UserDetails userDetails = entryService.loadUserByUsername(user.getEmail());
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException e) {
            System.out.println("Key validation failed and might be security Breach");
            System.out.println(e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}
