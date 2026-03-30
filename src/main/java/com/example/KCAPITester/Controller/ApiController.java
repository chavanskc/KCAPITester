package com.example.KCAPITester.Controller;

import com.example.KCAPITester.Service.ApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * REST Controller for handling API testing requests.
 * This controller provides endpoints for testing GET and POST API calls
 * with various parameter configurations and request types.
 */
@RestController
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^{}]+}}");

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }
    /*@GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome to API Tester!");
        return "index";
    }*/

    /**
     * Constructs a complete URL with query parameters.
     * 
     * @param baseUrl The base URL without parameters
     * @param paramKeys List of parameter keys
     * @param paramValues List of parameter values (must match size of paramKeys)
     * @return Complete URL with query parameters appended
     */
    public String getCompleteUrl(String baseUrl, List<String> paramKeys, List<String> paramValues) {
        log.debug("Building URL - baseUrl: {}, paramCount: {}", baseUrl, paramKeys.size());
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        
        // Append query parameters if they exist and counts match
        if (!paramKeys.isEmpty() && paramKeys.size() == paramValues.size()) {
            urlBuilder.append("?");
            for (int i = 0; i < paramKeys.size(); i++) {
                urlBuilder.append(paramKeys.get(i)).append("=").append(paramValues.get(i)).append("&");
            }
            urlBuilder.setLength(urlBuilder.length() - 1); // Remove trailing '&'
            log.debug("URL constructed successfully with {} parameters", paramKeys.size());
        } else {
            log.warn("Parameter key and value count mismatch - Keys: {}, Values: {}", paramKeys.size(), paramValues.size());
        }
        
        return urlBuilder.toString();
    }

    @GetMapping("/env-vars")
    public ResponseEntity<Map<String, String>> getEnvVariables() {
        Map<String, String> envVars = apiService.loadEnvVariables();
        return ResponseEntity.ok(envVars);
    }

    @PutMapping("/env-vars")
    public ResponseEntity<Map<String, Object>> saveEnvVariables(@RequestBody Map<String, String> envVars) {
        Map<String, Object> response = new LinkedHashMap<>();

        for (String key : envVars.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                response.put("message", "Environment variable key cannot be blank.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            if (key.contains("#")) {
                response.put("message", "Environment variable key cannot contain #.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        Map<String, String> saved = apiService.saveEnvVariables(envVars);
        response.put("message", "Environment variables saved.");
        response.put("envVars", saved);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles GET API requests with query parameters.
     * Constructs the URL with provided parameters and executes the GET request.
     * 
     * @param baseUrl The base URL of the API endpoint
     * @param paramKeys List of query parameter keys
     * @param paramValues List of query parameter values
     * @param model Spring model for adding response attributes
     * @return API response as string, or error message if request fails
     */
    @RequestMapping(value = "/test-api", method = RequestMethod.GET)
    public String handleGetRequest(
            @RequestParam String baseUrl,
            @RequestParam("paramKeys") List<String> paramKeys,
            @RequestParam("paramValues") List<String> paramValues,
            Model model) {

        log.info("GET API called with baseUrl: {}, paramKeys: {}, paramValues: {}", baseUrl, paramKeys, paramValues);
        String response;
        
        // Validate base URL
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.error("Validation failed: Base URL is missing or empty");
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing.");
            response = "Base URL is missing.";
            return response;
        }

        if (hasUnresolvedTemplate(baseUrl) || hasUnresolvedTemplateList(paramKeys) || hasUnresolvedTemplateList(paramValues)) {
            log.warn("GET request contains unresolved template values.");
            return "Error occurred: unresolved environment placeholders found in request.";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = getCompleteUrl(baseUrl, paramKeys, paramValues);

            log.info("Constructed GET URL: {}", url);
            log.debug("Sending GET request to API endpoint");

            response = restTemplate.getForObject(url, String.class);
            log.info("GET API call successful. Response length: {} characters", response == null ? 0 : response.length());
            log.debug("GET API Response: {}", response);

            return response;
        } catch (Exception e) {
            log.error("Error occurred while calling GET API: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            response = "Error occurred: " + e.getMessage();
        }
        return response;
    }

    /**
     * Handles POST API requests with JSON content.
     * Accepts JSON body containing baseUrl and other data for the API call.
     * 
     * @param jsonInput JSON map containing the base URL and request body
     * @param model Spring model for adding response attributes
     * @return API response as string, or error message if request fails
     */
    @RequestMapping(value = "/test-api", method = RequestMethod.POST, consumes = "application/json")
    public String handlePostJsonRequest(
            @RequestBody Map<String, Object> jsonInput,
            Model model) {

        log.info("POST API called with JSON input: {}", jsonInput);
        String response;

        // Validate required baseUrl field
        if (!jsonInput.containsKey("baseUrl")) {
            log.error("Validation failed: baseUrl field is missing from JSON input");
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing in JSON.");
            return "Base URL is missing in JSON.";
        }

        if (containsUnresolvedTemplate(jsonInput)) {
            log.warn("POST JSON request contains unresolved template values.");
            return "Error occurred: unresolved environment placeholders found in request.";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String baseUrl = (String) jsonInput.get("baseUrl");
            
            log.info("Extracted baseUrl from JSON: {}", baseUrl);
            log.debug("JSON payload (excluding response): {}", jsonInput);
            log.debug("Sending POST request with JSON body to API endpoint");

            response = restTemplate.postForObject(baseUrl, jsonInput, String.class);
            log.info("POST API call successful. Response length: {} characters", response == null ? 0 : response.length());
            log.debug("POST API Response: {}", response);

            return response;
        } catch (Exception e) {
            log.error("Error occurred while calling POST API with JSON: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            response = "Error occurred: " + e.getMessage();

        }
        return response;
    }

    /**
     * Handles POST API requests with form URL-encoded content.
     * Constructs the URL with provided parameters and executes the POST request.
     * 
     * @param baseUrl The base URL of the API endpoint
     * @param paramKeys List of form parameter keys
     * @param paramValues List of form parameter values
     * @param model Spring model for adding response attributes
     * @return API response as string, or error message if request fails
     */
    @RequestMapping(value = "/test-api", method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    public String handlePostFormRequest(
            @RequestParam String baseUrl,
            @RequestParam("paramKeys") List<String> paramKeys,
            @RequestParam("paramValues") List<String> paramValues,
            Model model) {

        log.info("POST API called with baseUrl: {}, paramKeys: {}, paramValues: {}", baseUrl, paramKeys, paramValues);
        String response;
        
        // Validate base URL
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.error("Validation failed: Base URL is missing or empty");
            model.addAttribute("response", null);
            model.addAttribute("error", "Base URL is missing.");
            return "index";
        }

        if (hasUnresolvedTemplate(baseUrl) || hasUnresolvedTemplateList(paramKeys) || hasUnresolvedTemplateList(paramValues)) {
            log.warn("POST form request contains unresolved template values.");
            return "Error occurred: unresolved environment placeholders found in request.";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = getCompleteUrl(baseUrl, paramKeys, paramValues);

            log.info("Constructed POST URL: {}", url);
            log.debug("Sending POST request with form parameters to API endpoint");

            response = restTemplate.postForObject(url, null, String.class);
            log.info("POST API call successful. Response length: {} characters", response == null ? 0 : response.length());
            log.debug("POST API Response: {}", response);

            model.addAttribute("response", response);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error occurred while calling POST API: {} - Exception type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            response = "Error occurred: " + e.getMessage();
            log.debug("Adding error response to model");
        }
        return response;
    }
    /**
     * Handles error page requests.
     * Logs the error and returns an error view with appropriate message.
     * 
     * @param model Spring model for adding error message
     * @return Error page view name
     */
    @GetMapping("/error")
    public String handleError(Model model) {
        log.error("An error occurred while processing API request");
        log.debug("Setting error message and returning error view");
        model.addAttribute("message", "An error occurred while processing your request.");
        return "index";
    }

    private boolean hasUnresolvedTemplate(String value) {
        return value != null && TEMPLATE_PATTERN.matcher(value).find();
    }

    private boolean hasUnresolvedTemplateList(List<String> values) {
        for (String value : values) {
            if (hasUnresolvedTemplate(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUnresolvedTemplate(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return hasUnresolvedTemplate(stringValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            for (Object entryValue : mapValue.values()) {
                if (containsUnresolvedTemplate(entryValue)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof List<?> listValue) {
            for (Object listItem : listValue) {
                if (containsUnresolvedTemplate(listItem)) {
                    return true;
                }
            }
        }
        return false;
    }
}
