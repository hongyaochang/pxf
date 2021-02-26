package org.greenplum.pxf.service;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class PxfConfigurationTest {

    private PxfConfiguration configuration;
    private WebMvcTagsContributor contributor;
    private HttpServletRequest mockRequest;

    @BeforeEach
    public void setup() {
        configuration = new PxfConfiguration(null, true);
        contributor = configuration.webMvcTagsContributor();
        mockRequest = mock(HttpServletRequest.class, withSettings().lenient());
    }

    @Test
    public void testPxfWebMvcTagsContributor_pxfEndpoint_namedServer() {
        when(mockRequest.getHeader("X-GP-USER")).thenReturn("Alex");
        when(mockRequest.getHeader("X-GP-SEGMENT-ID")).thenReturn("5");
        when(mockRequest.getHeader("X-GP-OPTIONS-PROFILE")).thenReturn("test:text");
        when(mockRequest.getHeader("X-GP-OPTIONS-SERVER")).thenReturn("test_server");

        List<Tag> expectedTags = Tags.of("user", "Alex")
                .and("segment", "5")
                .and("profile", "test:text")
                .and("server", "test_server")
                .stream().collect(Collectors.toList());

        Iterable<Tag> tagsIterable = contributor.getTags(mockRequest, null, null, null);
        List<Tag> tags = StreamSupport.stream(tagsIterable.spliterator(), false).collect(Collectors.toList());

        assertTrue(CollectionUtils.isEqualCollection(expectedTags, tags));
        assertFalse(contributor.getLongRequestTags(mockRequest, null).iterator().hasNext());
    }

    @Test
    public void testPxfWebMvcTagsContributor_pxfEndpoint_defaultServer() {
        when(mockRequest.getHeader("X-GP-USER")).thenReturn("Alex");
        when(mockRequest.getHeader("X-GP-SEGMENT-ID")).thenReturn("5");
        when(mockRequest.getHeader("X-GP-OPTIONS-PROFILE")).thenReturn("test:text");
        when(mockRequest.getHeader("X-GP-OPTIONS-SERVER")).thenReturn(null);

        List<Tag> expectedTags = Tags.of("user", "Alex")
                .and("segment", "5")
                .and("profile", "test:text")
                .and("server", "default")
                .stream().collect(Collectors.toList());

        Iterable<Tag> tagsIterable = contributor.getTags(mockRequest, null, null, null);
        List<Tag> tags = StreamSupport.stream(tagsIterable.spliterator(), false).collect(Collectors.toList());

        assertTrue(CollectionUtils.isEqualCollection(expectedTags, tags));
        assertFalse(contributor.getLongRequestTags(mockRequest, null).iterator().hasNext());
    }

    @Test
    public void testPxfWebMvcTagsContributor_pxfEndpoint_tagsDisabled() {
        configuration = new PxfConfiguration(null, false);
        contributor = configuration.webMvcTagsContributor();
        when(mockRequest.getHeader("X-GP-USER")).thenReturn("Alex");
        when(mockRequest.getHeader("X-GP-SEGMENT-ID")).thenReturn("5");
        when(mockRequest.getHeader("X-GP-OPTIONS-PROFILE")).thenReturn("test:text");
        when(mockRequest.getHeader("X-GP-OPTIONS-SERVER")).thenReturn(null);

        assertFalse(contributor.getTags(mockRequest, null, null, null).iterator().hasNext());
        assertFalse(contributor.getLongRequestTags(mockRequest, null).iterator().hasNext());
    }

    @Test
    public void testPxfWebMvcTagsContributor_nonPxfEndpoint() {
        when(mockRequest.getHeader("X-GP-USER")).thenReturn(null);

        Iterable<Tag> tagsIterable = contributor.getTags(mockRequest, null, null, null);
        assertFalse(tagsIterable.iterator().hasNext());
        assertFalse(contributor.getLongRequestTags(mockRequest, null).iterator().hasNext());
    }

}
