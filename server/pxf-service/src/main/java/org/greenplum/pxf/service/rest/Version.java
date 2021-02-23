package org.greenplum.pxf.service.rest;

/**
 * PXF protocol version. Any call to PXF resources should include the current
 * version e.g. {@code ...pxf/v15/Bridge}
 */
public class Version {
    /**
     * Constant which holds current protocol version. Getting replaced with
     * actual value on build stage, using pxfProtocolVersion parameter from
     * gradle.properties
     */
    // TODO: this (^) comment lies
    // TODO: add comment to keep this file in sync w/ api_version in repo root
    public final static String PXF_PROTOCOL_VERSION = "16";
}
