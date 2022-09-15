package org.apache.nifi.hbase;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.nifi.hbase.put.PutColumn;
import org.apache.nifi.hbase.put.PutFlowFile;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TestHbaseClientCustom {
    static final String COL_FAMILY = "f";

    @Test
    public void testSinglePut() throws InitializationException, IOException {
        final String tableName = "nifi";
        final String row = "row1";
        final String columnFamily = "f";
        final String columnQualifier = "q";
        final String content = "filetest";

        final Collection<PutColumn> columns = Collections.singletonList(new PutColumn(columnFamily.getBytes(StandardCharsets.UTF_8),
                columnQualifier.getBytes(StandardCharsets.UTF_8),
                content.getBytes(StandardCharsets.UTF_8)));
        final PutFlowFile putFlowFile = new PutFlowFile(tableName, row.getBytes(StandardCharsets.UTF_8), columns, null);

        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);

        // Mock an HBase Table so we can verify the put operations later
        final Table table = Mockito.mock(Table.class);
        when(table.getName()).thenReturn(TableName.valueOf(tableName));

        HBase_2_ClientService hBaseClientService = new HBase_2_ClientService();
        runner.addControllerService("hbaseClient", hBaseClientService);
        runner.setProperty(hBaseClientService,"ZooKeeper Quorum", "hadoop23124,hadoop23200,hadoop23201");
        runner.setProperty(hBaseClientService,"ZooKeeper Client Port","2182");
        runner.setProperty(hBaseClientService,"ZooKeeper ZNode Parent","/hbase");
        runner.setProperty(hBaseClientService,"HBase Client Retries","3");
        runner.enableControllerService(hBaseClientService);
        runner.setProperty(TestProcessor.HBASE_CLIENT_SERVICE, "hbaseClient");
        HBaseClientService hBaseClientService1 = runner.getProcessContext().getProperty(TestProcessor.HBASE_CLIENT_SERVICE).asControllerService(HBaseClientService.class);
        hBaseClientService1.put(tableName, Arrays.asList(putFlowFile));

    }

}
