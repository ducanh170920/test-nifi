package org.apache.nifi.processors.standard.util;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.processors.standard.ListFile;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;


public class TestListFile2 {
    private static boolean isMillisecondSupported = false;

    private TestRunner runner;
    private final String TESTDIR = "/home/anh/C++";


    @BeforeEach
    public void init() {
        runner = TestRunners.newTestRunner(ListFile.class);
    }

    private final ListFile listFile = new ListFile();

    @Test
    public void testSuccess() throws IOException, InitializationException {
        System.out.println("\n--- testSuccess() -----------------------------------------------------------------------");
        runner.setProperty(ListFile.DIRECTORY, TESTDIR);

        runner.run(1);
        runner.assertQueueEmpty();

        System.out.println(runner.getStateManager().getState(Scope.LOCAL).toMap());
        List<MockFlowFile> flowfiles = runner.getFlowFilesForRelationship(ListFile.REL_SUCCESS);

// we know there's only one flowfile, so get it for our test...
        for (MockFlowFile flowfile : flowfiles) {
            Map<String, String> attributes = flowfile.getAttributes();
            System.out.println(attributes);
        }
    }

}
