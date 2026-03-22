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

    @RequestMapping(value = "/test-api", method = RequestMethod.GET)
    public String handleGetRequest(
            @RequestParam String baseUrl,
            @RequestParam("paramKeys") List<String> paramKeys,
            @RequestParam("paramValues") List<String> paramValues,
            Model model) {

        log.info("GET API called with baseUrl: {}, paramKeys: {}, paramValues: {}", baseUrl, paramKeys, paramValues);

        if (baseUrl == null || baseUrl.isEmpty()) {
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing.");
            return "index";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            StringBuilder urlBuilder = new StringBuilder(baseUrl);

            if (!paramKeys.isEmpty() && paramKeys.size() == paramValues.size()) {
                urlBuilder.append("?");
                for (int i = 0; i < paramKeys.size(); i++) {
                    urlBuilder.append(paramKeys.get(i)).append("=").append(paramValues.get(i)).append("&");
                }
                urlBuilder.setLength(urlBuilder.length() - 1); // Remove trailing '&'
            }

            String url = urlBuilder.toString();
            log.info("Constructed GET URL: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            log.info("GET API call successful. Response: {}", response);

            model.addAttribute("response", response);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error occurred while calling GET API: {}", e.getMessage(), e);
            model.addAttribute("response", null);
            model.addAttribute("error", "Error occurred: " + e.getMessage());
        }
        return "index";
    }

    @RequestMapping(value = "/test-api", method = RequestMethod.POST)
    public String handlePostRequest(
            @RequestParam String baseUrl,
            @RequestParam("paramKeys") List<String> paramKeys,
            @RequestParam("paramValues") List<String> paramValues,
            Model model) {

        log.info("POST API called with baseUrl: {}, paramKeys: {}, paramValues: {}", baseUrl, paramKeys, paramValues);

        if (baseUrl == null || baseUrl.isEmpty()) {
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing.");
            return "index";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            StringBuilder urlBuilder = new StringBuilder(baseUrl);

            if (!paramKeys.isEmpty() && paramKeys.size() == paramValues.size()) {
                urlBuilder.append("?");
                for (int i = 0; i < paramKeys.size(); i++) {
                    urlBuilder.append(paramKeys.get(i)).append("=").append(paramValues.get(i)).append("&");
                }
                urlBuilder.setLength(urlBuilder.length() - 1); // Remove trailing '&'
            }

            String url = urlBuilder.toString();
            log.info("Constructed POST URL: {}", url);

            String response = restTemplate.postForObject(url, null, String.class);
            log.info("POST API call successful. Response: {}", response);

            model.addAttribute("response", response);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error occurred while calling POST API: {}", e.getMessage(), e);
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
