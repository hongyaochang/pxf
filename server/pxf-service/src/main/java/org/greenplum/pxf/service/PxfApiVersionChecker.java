package org.greenplum.pxf.service;

import org.apache.commons.lang.StringUtils;

public class PxfApiVersionChecker {

    public static boolean isCompatible(String version1, String version2) {
        return StringUtils.equals(version1, version2);
    }
}
