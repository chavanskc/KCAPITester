package com.example.KCAPITester.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiService {
	private static final Logger log = LoggerFactory.getLogger(ApiService.class);

	private static final String ENV_FILE_NAME = "ENv_Var.txt";
	private static final String DELIMITER = "#";
	private static final Path ENV_FILE_PATH = Paths.get("src", "main", "resources", ENV_FILE_NAME);

	public synchronized Map<String, String> loadEnvVariables() {
		ensureEnvFileExists();
		Map<String, String> envVars = new LinkedHashMap<>();

		try {
			List<String> lines = Files.readAllLines(ENV_FILE_PATH, StandardCharsets.UTF_8);
			for (String line : lines) {
				if (line == null || line.trim().isEmpty()) {
					continue;
				}

				int delimiterIndex = line.indexOf(DELIMITER);
				if (delimiterIndex <= 0) {
					log.warn("Skipping invalid env line: {}", line);
					continue;
				}

				String key = line.substring(0, delimiterIndex).trim();
				String value = line.substring(delimiterIndex + 1);
				if (!key.isEmpty()) {
					envVars.put(key, value);
				}
			}
		} catch (IOException ex) {
			log.error("Failed to read env file at {}", ENV_FILE_PATH.toAbsolutePath(), ex);
		}

		return envVars;
	}

	public synchronized Map<String, String> saveEnvVariables(Map<String, String> envVars) {
		ensureEnvFileExists();
		Map<String, String> sanitized = new LinkedHashMap<>();

		for (Map.Entry<String, String> entry : envVars.entrySet()) {
			String key = entry.getKey() == null ? "" : entry.getKey().trim();
			String value = entry.getValue() == null ? "" : entry.getValue();
			if (!key.isEmpty()) {
				sanitized.put(key, value);
			}
		}

		List<String> lines = sanitized.entrySet().stream()
				.map(entry -> entry.getKey() + DELIMITER + entry.getValue())
				.toList();

		try {
			Files.write(ENV_FILE_PATH, lines, StandardCharsets.UTF_8);
			log.info("Saved {} env variables to {}", sanitized.size(), ENV_FILE_PATH.toAbsolutePath());
		} catch (IOException ex) {
			log.error("Failed to write env file at {}", ENV_FILE_PATH.toAbsolutePath(), ex);
		}

		return sanitized;
	}

	private void ensureEnvFileExists() {
		try {
			Path parent = ENV_FILE_PATH.getParent();
			if (parent != null && !Files.exists(parent)) {
				Files.createDirectories(parent);
			}
			if (!Files.exists(ENV_FILE_PATH)) {
				Files.createFile(ENV_FILE_PATH);
				log.info("Created env file at {}", ENV_FILE_PATH.toAbsolutePath());
			}
		} catch (IOException ex) {
			log.error("Failed while preparing env file path {}", ENV_FILE_PATH.toAbsolutePath(), ex);
		}
	}
}
