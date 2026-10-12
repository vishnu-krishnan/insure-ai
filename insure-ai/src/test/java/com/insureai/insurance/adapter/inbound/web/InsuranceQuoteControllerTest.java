package com.insureai.insurance.adapter.inbound.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InsuranceQuoteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createQuoteReturnsCreatedResponseForValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerName").value("Alice Smith"))
            .andExpect(jsonPath("$.coverageType").value("AUTO"))
            .andExpect(jsonPath("$.annualPremium").isNumber());
    }

    @Test
    void createQuoteReturnsProblemDetailForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"A\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));
    }
}
