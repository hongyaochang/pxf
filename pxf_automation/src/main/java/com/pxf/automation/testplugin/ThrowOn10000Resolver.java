package com.pxf.automation.testplugin;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hawq.pxf.api.OneRow;
import org.apache.hawq.pxf.api.OneField;
import org.apache.hawq.pxf.api.utilities.InputData;
import org.apache.hawq.pxf.plugins.hdfs.StringPassResolver;

/**
 * Test resolver, based on StringPassResolver.
 * This resolver throws a runtime exception after reading 10000 rows.
 * Used to test an error during ANALYZE.
 */
public class ThrowOn10000Resolver extends StringPassResolver {

	private static Log Log = LogFactory.getLog(ThrowOn10000Resolver.class);
	private int rowCount;

    /**
     * Constructs a StringPassResolver
     *
     * @param inputData input all input parameters coming from the client request
     */
    public ThrowOn10000Resolver(InputData inputData) {
    	super(inputData);

        rowCount = 0;
    }

    /**
     * Resolves next row using StringPassResolver.getFields().
     * Throws a runtime exception upon reading the 10000 record.
     *
     * @param onerow row to be resolved
     * @return row resolved into fields
     * @throws RuntimeException on the 10000 row (expected)
     */
    @Override
    public List<OneField> getFields(OneRow onerow) {

    	List<OneField> record = super.getFields(onerow);

    	if ((record != null) && (rowCount == 10000)) {
    		throw new RuntimeException("10000 rows!");
    	}
    	rowCount++;

        return record;
    }
}
