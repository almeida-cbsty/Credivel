package org.credivel.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {
    @GetMapping({"/login", "/publications", "/news/{slug}"})
    public String frontend() {
        return "forward:/index.html";
    }
}
