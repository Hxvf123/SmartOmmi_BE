package com.smartomni.ai.client;

import com.smartomni.ai.dto.ForecastResultDto;
import com.smartomni.ai.dto.RecommendationResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Client goi sang AI Microservice viet bang Python (FastAPI) - noi thuc su
 * chay Prophet/ARIMA (UC-14/UC-35) va tinh Cosine Similarity (UC-07/UC-36).
 * Repo Python duoc quan ly rieng, xem README goc de biet dia chi repo do.
 *
 * TODO: cau hinh timeout/retry hop ly vi cac tac vu training co the mat vai giay-vai phut;
 * can nhac goi bat dong bo (queue job) thay vi cho dong bo qua WebClient neu training lau.
 */
@Component
public class PythonAiServiceClient {

    private final WebClient webClient;

    public PythonAiServiceClient(@Value("${smartomni.ai-python-service.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** UC-14/UC-35: yeu cau Python service chay Demand Forecasting cho 1 Tenant. */
    public List<ForecastResultDto> requestDemandForecast(Long tenantId) {
        return webClient.post()
                .uri("/forecast/demand")
                .bodyValue(new ForecastRequestBody(tenantId))
                .retrieve()
                .bodyToFlux(ForecastResultDto.class)
                .collectList()
                .block(); // TODO: can nhac chuyen sang reactive non-blocking hoan toan neu can throughput cao
    }

    /** UC-07/UC-36: yeu cau tinh Cosine Similarity, tra ve top-N san pham lien quan. */
    public List<RecommendationResultDto> requestRecommendations(Long tenantId, Long skuId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/recommendations")
                        .queryParam("tenantId", tenantId)
                        .queryParam("skuId", skuId)
                        .build())
                .retrieve()
                .bodyToFlux(RecommendationResultDto.class)
                .collectList()
                .block();
    }

    private record ForecastRequestBody(Long tenantId) {}
}
