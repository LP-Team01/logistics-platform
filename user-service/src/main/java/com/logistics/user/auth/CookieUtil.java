package com.logistics.user.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {


    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge){
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 로컬 실행을 위해 임시 false https 구현시 true로
        cookie.setPath("/");
        response.addCookie(cookie);
    }


    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name){
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for(Cookie cookie:cookies){
                if(cookie.getName().equals(name)){
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setValue(null);
                    response.addCookie(cookie);
                }
            }
        }
    }


    public static Optional<Cookie> getCookie(HttpServletRequest request, String name){
        if(request.getCookies() == null){
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookie.getName().equals(name))
            .findFirst();
    }
}
