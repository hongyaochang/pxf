package org.greenplum.pxf.service;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.greenplum.pxf.api.BadRecordException;
import org.greenplum.pxf.api.OneField;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.model.Accessor;
import org.greenplum.pxf.api.model.Plugin;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.api.model.Resolver;
import org.greenplum.pxf.api.utilities.Utilities;
import org.greenplum.pxf.service.io.Writable;

import java.io.DataInputStream;
import java.util.List;

/*
 * WriteBridge class creates appropriate accessor and resolver.
 * It reads data from inputStream by the resolver,
 * and writes it to the Hadoop storage with the accessor.
 */
public class WriteBridge implements Bridge {
    private static final Log LOG = LogFactory.getLog(WriteBridge.class);
    private final Accessor fileAccessor;
    private final Resolver fieldsResolver;
    private final BridgeInputBuilder inputBuilder;

    /*
     * C'tor - set the implementation of the bridge
     */
    public WriteBridge(RequestContext context) throws Exception {

        inputBuilder = new BridgeInputBuilder(context);
        /* plugins accept RequestContext parameters */
        fileAccessor = getFileAccessor(context);
        fieldsResolver = getFieldsResolver(context);

    }

    /*
     * Accesses the underlying HDFS file
     */
    @Override
    public boolean beginIteration() throws Exception {
        return fileAccessor.openForWrite();
    }

    /*
     * Read data from stream, convert it using Resolver into OneRow object, and
     * pass to WriteAccessor to write into file.
     */
    @Override
    public boolean setNext(DataInputStream inputStream) throws Exception {

        List<OneField> record = inputBuilder.makeInput(inputStream);
        if (record == null) {
            return false;
        }

        OneRow onerow = fieldsResolver.setFields(record);
        if (onerow == null) {
            return false;
        }
        if (!fileAccessor.writeNextObject(onerow)) {
            throw new BadRecordException();
        }
        return true;
    }

    /*
     * Close the underlying resource
     */
    public void endIteration() throws Exception {
        try {
            fileAccessor.closeForWrite();
        } catch (Exception e) {
            LOG.error("Failed to close bridge resources: " + e.getMessage());
            throw e;
        }
    }

    private static Accessor getFileAccessor(RequestContext context) throws Exception {
        return (Accessor) Utilities.createAnyInstance(RequestContext.class, context.getAccessor(), context);
    }

    private static Resolver getFieldsResolver(RequestContext context) throws Exception {
        return (Resolver) Utilities.createAnyInstance(RequestContext.class, context.getResolver(), context);
    }

    @Override
    public Writable getNext() {
        throw new UnsupportedOperationException("getNext is not implemented");
    }

    @Override
    public boolean isThreadSafe() {
        return ((Plugin) fileAccessor).isThreadSafe() && ((Plugin) fieldsResolver).isThreadSafe();
    }
}
