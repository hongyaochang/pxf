package com.pxf.automation.testplugin;

import org.apache.hawq.pxf.api.Fragment;
import org.apache.hawq.pxf.api.Fragmenter;
import org.apache.hawq.pxf.api.FragmentsStats;
import org.apache.hawq.pxf.api.utilities.InputData;

import java.util.List;

/*
 * Class that defines the splitting of a data resource into fragments that can
 * be processed in parallel
 * getFragments() returns the fragments information of a given path (source name and location of each fragment).
 * Used to get fragments of data that could be read in parallel from the different segments.
 * Dummy implementation, for documentation
 */
public class DummyFragmenter extends Fragmenter {
    public DummyFragmenter(InputData metaData) {
        super(metaData);
    }

    /*
     * path is a data source URI that can appear as a file name, a directory name  or a wildcard
     * returns the data fragments - identifiers of data and a list of available hosts
     */
    @Override
    public List<Fragment> getFragments() throws Exception {
        String localhostname = java.net.InetAddress.getLocalHost().getHostName();
        String[] localHosts = new String[]{localhostname, localhostname};
        fragments.add(new Fragment(inputData.getDataSource() + ".1" /* source name */,
                localHosts /* available hosts list */,
                "fragment1".getBytes()));
        fragments.add(new Fragment(inputData.getDataSource() + ".2" /* source name */,
                localHosts /* available hosts list */,
                "fragment2".getBytes()));
        fragments.add(new Fragment(inputData.getDataSource() + ".3" /* source name */,
                localHosts /* available hosts list */,
                "fragment3".getBytes()));
        return fragments;
    }

    @Override
    public FragmentsStats getFragmentsStats() throws Exception {
        return new FragmentsStats(3, 10000000, 100000000L);
    }
}
