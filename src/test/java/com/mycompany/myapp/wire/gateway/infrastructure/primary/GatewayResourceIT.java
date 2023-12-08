package com.mycompany.myapp.wire.gateway.infrastructure.primary;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mycompany.myapp.wire.gateway.infrastructure.primary.vm.RouteVM;
import java.net.URI;
import java.util.function.Predicate;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@WebFluxTest(GatewayResource.class)
@AutoConfigureWebTestClient
class GatewayResourceIT {

  @MockBean
  private DiscoveryClient discoveryClient;

  @MockBean
  private RouteLocator routeLocator;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebTestClient webTestClient;

  @Value("${spring.application.name}")
  private String appName;

  @BeforeEach
  public void setUp() {
    SimpleModule simpleModule = new SimpleModule().addAbstractTypeMapping(ServiceInstance.class, DefaultServiceInstance.class);
    objectMapper.registerModule(simpleModule);
  }

  @Test
  void shouldGetActiveRoutes() throws Exception {
    Flux<Route> routes = Flux.just(
      Route
        .async()
        .id("ReactiveCompositeDiscoveryClient_MY-APP")
        .uri(new URI("lb://MY-APP"))
        .asyncPredicate(new AsyncPredicate.DefaultAsyncPredicate<>(this.getPredicate()))
        .build()
    );
    when(routeLocator.getRoutes()).thenReturn(routes);

    DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
    serviceInstance.setInstanceId("APP");
    serviceInstance.setServiceId("app:7459517c1334fe03658ce69eb959c7bd");
    serviceInstance.setUri(new URI("http://192.168.1.38:8082"));
    serviceInstance.setHost("192.168.1.38");
    serviceInstance.setPort(8082);
    when(discoveryClient.getInstances("my-app")).thenReturn(singletonList(serviceInstance));

    RouteVM route = webTestClient
      .get()
      .uri(new URI("/api/gateway/routes"))
      .exchange()
      .expectStatus()
      .isOk()
      .returnResult(RouteVM.class)
      .getResponseBody()
      .blockFirst();

    assertThat(route.getServiceId()).isEqualTo("my-app");
    assertThat(route.getPath()).isEqualTo("'/services/' + serviceId.toLowerCase() +'/**'");
    assertThat(route.getServiceInstances())
      .hasSize(1)
      .extracting(
        ServiceInstance::getInstanceId,
        ServiceInstance::getServiceId,
        ServiceInstance::getUri,
        ServiceInstance::getHost,
        ServiceInstance::getPort
      )
      .contains(
        tuple("APP", "app:7459517c1334fe03658ce69eb959c7bd", new URI("http://192.168.1.38:8082"), "192.168.1.38", 8082),
        Index.atIndex(0)
      );
  }

  @Test
  void shouldExcludeGatewayAppFromActiveRoutes() throws Exception {
    Flux<Route> routes = Flux.just(
      Route
        .async()
        .id("ReactiveCompositeDiscoveryClient_" + appName.toUpperCase())
        .uri(new URI("lb://" + appName.toUpperCase()))
        .asyncPredicate(new AsyncPredicate.DefaultAsyncPredicate<>(this.getPredicate()))
        .build()
    );
    when(routeLocator.getRoutes()).thenReturn(routes);

    FluxExchangeResult<RouteVM> result = webTestClient
      .get()
      .uri(new URI("/api/gateway/routes"))
      .exchange()
      .expectStatus()
      .isOk()
      .returnResult(RouteVM.class);

    StepVerifier.create(result.getResponseBody()).expectComplete();
  }

  private Predicate<ServerWebExchange> getPredicate() {
    return new PathRoutePredicateFactory()
      .apply(new PathRoutePredicateFactory.Config().setPatterns(singletonList("'/services/' + serviceId.toLowerCase() +'/**'")));
  }
}
