package com.example.crypto_scout;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.crypto_scout.Controller.OpenAIController;
import com.example.crypto_scout.Service.OpenAIService;

@SpringBootTest
class CryptoScoutApplicationTests {

	@Test
	void contextLoads() {
	}

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private OpenAIService openAIService;

	@InjectMocks
	private OpenAIController openAIController;

	

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
