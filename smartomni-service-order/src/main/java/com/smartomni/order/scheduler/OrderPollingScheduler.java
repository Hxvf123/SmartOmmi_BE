package com.smartomni.order.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UC-34: Dong bo du phong qua Polling (Fallback Sync).
 * FR-063: chay dinh ky 2-5 phut, doi chieu don hang cho tung Tenant dang hoat dong.
 * FR-064: Retry voi Exponential Backoff khi goi API GetOrderList that bai.
 *
 * TODO trien khai that:
 *  1. Lay danh sach Tenant dang ACTIVE + co ket noi san TMDT hop le (goi Tenant Service).
 *  2. Voi moi Tenant, goi IntegrationServiceClient.getOrderList(tenantId, platform).
 *  3. Doi chieu voi OrderRepository (theo platform_order_id) de tim don hang bi thieu.
 *  4. Voi don hang thieu, goi lai OrderService.processIncomingOrder(..., OrderSource.POLLING).
 */
@Slf4j
@Component
public class OrderPollingScheduler {

    public void pollOrdersFromMarketplaces() {
        log.debug("Bat dau chu ky Polling doi chieu don hang (Fallback Sync - UC-34)");
        // TODO: implement nhu mo ta o javadoc phia tren
    }
}
