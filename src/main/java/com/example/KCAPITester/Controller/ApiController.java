package com.example.KCAPITester.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Slf4j
@Controller
public class ApiController {
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome to API Tester!");
        return "index";
    }

    @RequestMapping(value = "/test-api", method = {RequestMethod.GET, RequestMethod.POST})
    public String testApi(
            @RequestParam String method,
            @RequestParam String baseUrl,
            @RequestParam("paramKeys") List<String> paramKeys,
            @RequestParam("paramValues") List<String> paramValues,
            Model model) {

        log.info("Test API called with method: {}, baseUrl: {}, params Keys: {} , params Values: {} ", method, baseUrl, paramKeys , paramValues);

        if (baseUrl == null || baseUrl.isEmpty()) {
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing.");
            return "index";
        }
        try {

            RestTemplate restTemplate = new RestTemplate();
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            // Append dynamic parameters to the URL
            // create URL as below using the parameters
            //"http://localhost:8081/calculator/add?num1=5&num2=3"
            if (!paramKeys.isEmpty() && paramKeys.size() == paramValues.size()) {
                urlBuilder.append("?");
                for (int i = 0; i < paramKeys.size(); i++) {
                    urlBuilder.append(paramKeys.get(i)).append("=").append(paramValues.get(i)).append("&");
                }
                urlBuilder.setLength(urlBuilder.length() - 1); // Remove trailing '&'
            }
            log.info("Constructed URL: {}", urlBuilder.toString());

            String url = urlBuilder.toString();
            String response;
            log.info("Constructed URL: {}", url);

            if ("GET".equalsIgnoreCase(method)) {
                response = restTemplate.getForObject(url, String.class);
            } else if ("POST".equalsIgnoreCase(method)) {
                response = restTemplate.postForObject(url, null, String.class);
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            log.info("API call successful. Response: {}", response);
            model.addAttribute("response", response);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error occurred while calling API: {}", e.getMessage(), e);
            model.addAttribute("response", null);
            model.addAttribute("error", "Error occurred: " + e.getMessage());
        }
        return "index";
    }
    @GetMapping("/error")
    public String handleError(Model model) {
        log.error("An error occurred.");
        model.addAttribute("message", "An error occurred while processing your request.");
        return "index";
    }
}
