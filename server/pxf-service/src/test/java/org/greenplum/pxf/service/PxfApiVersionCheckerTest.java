package org.greenplum.pxf.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PxfApiVersionCheckerTest {

    private static Stream<Arguments> provideVersions() {
        return Stream.of(
                Arguments.of("1.0.0", "1.0.0", true),
                Arguments.of("1.1.0", "1.0.0", false),
                Arguments.of("1.0.0", "1.1.0", false),
                Arguments.of("1.0.0", "1.0.1", false),
                Arguments.of(null, "1.0.0", false),
                Arguments.of("1.0.0", null, false),
                Arguments.of(null, null, true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideVersions")
    public void testApiVersionsMatch(String extensionApiVersion, String serverApiVersion, boolean expected) {
        assertEquals(expected, PxfApiVersionChecker.isCompatible(extensionApiVersion, serverApiVersion));
    }
}
