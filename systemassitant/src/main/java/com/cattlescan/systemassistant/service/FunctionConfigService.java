package com.cattlescan.systemassistant.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;



@Service
public class FunctionConfigService {
    private JSONArray functions;

    @PostConstruct
    public void init() throws IOException {
        try {
            // Load function definitions from config file (or classpath)
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> config = mapper.readValue(
                    new File("C:\\tmp\\AI-assitantant\\assistant_functions.json"),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            this.functions = new JSONArray(config);
        } catch (Exception e) {
            e.printStackTrace();
            this.functions = new JSONArray(); // empty if load fails
        }
    }

    public JSONArray getFunctions() {
        return functions;
    }
}
