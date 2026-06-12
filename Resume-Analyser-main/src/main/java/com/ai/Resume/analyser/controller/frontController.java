package com.ai.Resume.analyser.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class frontController {

    @RequestMapping(value = {"/","/login","/forgotpassword","/uploaddoc","/analysereport","/analysisprogress","/jobprogress","/jdmatch","/dashboard","/admin","/simulation","/free-result"})
    public String forward(){
        return "forward:/index.html";
    }
}
