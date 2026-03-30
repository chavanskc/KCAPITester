package com.example.KCAPITester.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome to API Tester!");
        return "index";   // loads index.html ✅
    }
}
