package com.example.crypto_scout;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.context.annotation.Import;

import com.example.crypto_scout.Controller.OpenAIController;
import com.example.crypto_scout.Service.OpenAIService;

@WebMvcTest(OpenAIController.class)
@AutoConfigureMockMvc
@Import(OpenAITestConfig.class)
class CryptoScoutApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenAIService openAIService;

    @Test
    void contextLoads() {
    }

    @Test
    void testAnalyzeMarket() throws Exception {
        String inputData = "{\"cryptoData\": \"Bitcoin price analysis\"}";
        String mockResponse = "Bitcoin is showing bullish trends";

        when(openAIService.analyzeMarketTrends("Bitcoin price analysis")).thenReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/openai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inputData))
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));
    }
}
