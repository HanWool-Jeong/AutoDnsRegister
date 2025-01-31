package com.example.AutoDnsRegister;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JsonTest {

    @Test
    public void jsonTest() {
        String jsonInput = "{\"access_token\": \"1/fFAGRNJru1FTz70BzhT3Zg\", \"expires_in\": 3920, \"token_type\": \"Bearer\", \"scope\": \"https://www.googleapis.com/auth/drive.metadata.readonly https://www.googleapis.com/auth/calendar.readonly\", \"refresh_token\": \"1//xEoDL4iW3cxlI7yDbSRFYNG01kVKM2C-259HOF2aQbI\"}";
        try {
            JSONObject jsonObject = new JSONObject(jsonInput);
            System.out.println("안녕~~~");
            System.out.println(jsonObject.getString("access_token"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
