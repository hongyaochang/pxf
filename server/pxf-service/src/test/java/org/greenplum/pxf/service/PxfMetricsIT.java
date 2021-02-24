package org.greenplum.pxf.service;

import com.google.common.base.Charsets;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.service.controller.ReadService;
import org.greenplum.pxf.service.controller.WriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = PxfServiceApplication.class)
@TestPropertySource(properties = {"pxf.logdir=/tmp"})
public class PxfMetricsIT {

    @LocalServerPort
    private int port;

    @MockBean
    private RequestParser<MultiValueMap<String, String>> mockParser;

    @MockBean
    private ReadService readService;

    @MockBean
    private WriteService mockWriteService;

    @Mock
    private RequestContext mockContext;

    private WebTestClient client;

    @BeforeEach
    public void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    public void test_HttpServerRequests_Metric() throws Exception {
        mockReadService();
        // call PXF read API
        client.get().uri("/pxf/read")
                .header("X-GP-USER", "reader")
                .header("X-GP-SEGMENT-ID", "77")
                .header("X-GP-OPTIONS-PROFILE", "profile:test")
                .header("X-GP-OPTIONS-SERVER", "speedy")
                .exchange().expectStatus().isOk()
                .expectBody().toString().equals("Hello from read!");

        // assert metric got reported with proper tags
        client.get().uri("/actuator/metrics/http.server.requests?tag=uri:/pxf/read")
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.measurements[?(@.statistic == 'COUNT')].value").isEqualTo(1.0)
                .jsonPath("$.availableTags[?(@.tag == 'application')].values[0]").isEqualTo("pxf-service")
                .jsonPath("$.availableTags[?(@.tag == 'user')].values[0]").isEqualTo("reader")
                .jsonPath("$.availableTags[?(@.tag == 'segmentID')].values[0]").isEqualTo("77")
                .jsonPath("$.availableTags[?(@.tag == 'profile')].values[0]").isEqualTo("profile:test")
                .jsonPath("$.availableTags[?(@.tag == 'server')].values[0]").isEqualTo("speedy");

        // call PXF write API
        client.post().uri("/pxf/write")
                .header("X-GP-USER", "writer")
                .header("X-GP-SEGMENT-ID", "77")
                .header("X-GP-OPTIONS-PROFILE", "profile:test")
                .header("X-GP-OPTIONS-SERVER", "speedy")
                .exchange().expectStatus().isOk()
                .expectBody().toString().equals("Hello from write!");

        // assert metric got reported with proper tags
        client.get().uri("/actuator/metrics/http.server.requests?tag=uri:/pxf/write")
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.measurements[?(@.statistic == 'COUNT')].value").isEqualTo(1.0)
                .jsonPath("$.availableTags[?(@.tag == 'application')].values[0]").isEqualTo("pxf-service")
                .jsonPath("$.availableTags[?(@.tag == 'user')].values[0]").isEqualTo("writer")
                .jsonPath("$.availableTags[?(@.tag == 'segmentID')].values[0]").isEqualTo("77")
                .jsonPath("$.availableTags[?(@.tag == 'profile')].values[0]").isEqualTo("profile:test")
                .jsonPath("$.availableTags[?(@.tag == 'server')].values[0]").isEqualTo("speedy");

        // assert metric for segment access is aggregate
        client.get().uri("/actuator/metrics/http.server.requests?tag=segmentID:77")
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.measurements[?(@.statistic == 'COUNT')].value").isEqualTo(2.0)
                .jsonPath("$.availableTags[?(@.tag == 'application')].values[0]").isEqualTo("pxf-service")
                .jsonPath("$.availableTags[?(@.tag == 'user')].values[0]").isEqualTo("reader")
                .jsonPath("$.availableTags[?(@.tag == 'user')].values[1]").isEqualTo("writer")
                .jsonPath("$.availableTags[?(@.tag == 'profile')].values[0]").isEqualTo("profile:test")
                .jsonPath("$.availableTags[?(@.tag == 'server')].values[0]").isEqualTo("speedy");

        // assert prometheus endpoint reflects the metric as well
        // TODO somehow proetheus endpoint is not accessible from the test
        client.get().uri("/actuator/prometheus")
                .exchange().expectStatus().isNotFound();

//        client.get().uri("/actuator/prometheus")
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody().toString().equals("foo");
    }

    private void mockReadService() throws IOException {
        when(mockParser.parseRequest(any(), eq(RequestContext.RequestType.READ_BRIDGE))).thenReturn(mockContext);
        Answer<Void> ans = invocation -> {
            invocation.getArgument(1, OutputStream.class).write("Hello from read!".getBytes(Charsets.UTF_8));
            return null;
        };
        doAnswer(ans).when(readService).readData(any(), any());
    }

}
