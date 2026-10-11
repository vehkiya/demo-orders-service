package app.kerrlab.demo.orders.catalog;

import java.net.http.HttpClient;

import app.kerrlab.demo.orders.OrdersProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base URL and timeout come from {@code orders.catalog.*}. The base URL is {@code http://catalog:8080} in every
 * Kubernetes environment (see the kubernetes document in application.yaml), so it never needs a per-env override.
 */
@Configuration(proxyBeanMethods = false)
class CatalogClientConfig {

    @Bean
    CatalogClient catalogClient(OrdersProperties properties) {
        return client(restClientBuilder(properties.catalog()).build());
    }

    static RestClient.Builder restClientBuilder(OrdersProperties.Catalog catalog) {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(catalog.timeout())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(catalog.timeout());
        return RestClient.builder().baseUrl(catalog.baseUrl().toString()).requestFactory(requestFactory);
    }

    static CatalogClient client(RestClient restClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(CatalogClient.class);
    }
}
