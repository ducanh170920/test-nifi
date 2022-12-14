/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.ignite.cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestGetIgniteCache {

    private static final String CACHE_NAME = "testCache";
    private TestRunner getRunner;
    private GetIgniteCache getIgniteCache;
    private Map<String,String> properties1;
    private Map<String,String> properties2;
    private Ignite ignite;

    @BeforeEach
    public void setUp() {
        ignite = mock(Ignite.class);
        final IgniteCache igniteCache = mockIgniteCache();
        when(ignite.getOrCreateCache(or(ArgumentMatchers.eq(CACHE_NAME), isNull()))).thenReturn(igniteCache);
        getIgniteCache = new GetIgniteCache() {
            @Override
            protected Ignite getIgnite() {
                return ignite;
            }

        };

        properties1 = new HashMap<String,String>();
        properties1.put("igniteKey", "key1");
        properties2 = new HashMap<String,String>();
        properties2.put("igniteKey", "key2");

    }

    @AfterEach
    public void teardown() {
        getRunner = null;
    }

    static IgniteCache<Object, Object> mockIgniteCache() {
        final IgniteCache<Object, Object> igniteCache = mock(IgniteCache.class);
        final Map<Object, Object> map = new HashMap<>();
        doAnswer(args -> map.put(args.getArgument(0), args.getArgument(1))).when(igniteCache).put(any(), any());
        when(igniteCache.get(any())).thenAnswer(args -> map.get(args.getArgument(0)));
        when(igniteCache.containsKey(anyString())).thenAnswer(args -> map.containsKey(args.getArgument(0)));

        return igniteCache;
    }

    @Test
    public void testGetIgniteCacheDefaultConfOneFlowFileWithPlainKey() throws IOException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "mykey");

        getRunner.assertValid();
        getRunner.enqueue(new byte[] {});

        getIgniteCache.initialize(getRunner.getProcessContext());

        getIgniteCache.getIgniteCache().put("mykey", "test".getBytes());

        getRunner.run(1, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_SUCCESS, 1);
        List<MockFlowFile> getSucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(1, getSucessfulFlowFiles.size());
        List<MockFlowFile> getFailureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(0, getFailureFlowFiles.size());

        final MockFlowFile getOut = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);
        getOut.assertContentEquals("test".getBytes());

        getRunner.shutdown();
    }

    @Test
    public void testGetIgniteCacheNullGetCacheThrowsException() throws IOException, InterruptedException {

        getIgniteCache = new GetIgniteCache() {
            @Override
            protected Ignite getIgnite() {
                return ignite;
            }

            @Override
            protected IgniteCache<String, byte[]> getIgniteCache() {
                return null;
            }

        };
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "mykey");

        getRunner.assertValid();
        getRunner.enqueue(new byte[] {});

        getIgniteCache.initialize(getRunner.getProcessContext());

        getRunner.run(1, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_FAILURE, 1);
        List<MockFlowFile> getSucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(0, getSucessfulFlowFiles.size());
        List<MockFlowFile> getFailureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(1, getFailureFlowFiles.size());

        final MockFlowFile getOut = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);
        getOut.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY,
            GetIgniteCache.IGNITE_GET_FAILED_MESSAGE_PREFIX + "java.lang.NullPointerException");

        getRunner.shutdown();
    }

    @Test
    public void testGetIgniteCacheDefaultConfOneFlowFileWithKeyExpression() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.CACHE_NAME, CACHE_NAME);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        getRunner.enqueue("".getBytes(),properties1);

        getIgniteCache.initialize(getRunner.getProcessContext());

        getIgniteCache.getIgniteCache().put("key1", "test".getBytes());

        getRunner.run(1, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_SUCCESS, 1);
        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(1, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(0, failureFlowFiles.size());

        final MockFlowFile out = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);

        out.assertContentEquals("test".getBytes());
        getRunner.shutdown();
    }

    @Test
    public void testGetIgniteCacheDefaultConfTwoFlowFilesWithExpressionKeys() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.CACHE_NAME, CACHE_NAME);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        getRunner.enqueue("".getBytes(),properties1);
        getRunner.enqueue("".getBytes(),properties2);

        getIgniteCache.initialize(getRunner.getProcessContext());

        getIgniteCache.getIgniteCache().put("key1", "test1".getBytes());
        getIgniteCache.getIgniteCache().put("key2", "test2".getBytes());

        getRunner.run(2, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_SUCCESS, 2);

        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(2, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(0, failureFlowFiles.size());

        final MockFlowFile out1 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);

        out1.assertContentEquals("test1".getBytes());
        assertEquals("test1",new String(getIgniteCache.getIgniteCache().get("key1")));

        final MockFlowFile out2 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(1);

        out2.assertContentEquals("test2".getBytes());

        assertArrayEquals("test2".getBytes(),(byte[])getIgniteCache.getIgniteCache().get("key2"));

        getRunner.shutdown();
    }

    @Test
    public void testGetIgniteCacheDefaultConfOneFlowFileNoKey() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        properties1.clear();
        getRunner.enqueue("".getBytes(),properties1);
        getIgniteCache.initialize(getRunner.getProcessContext());

        getRunner.run(1, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_FAILURE, 1);
        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(0, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(1, failureFlowFiles.size());

        final MockFlowFile out = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);

        out.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);

        getRunner.shutdown();
    }



    @Test
    public void testGetIgniteCacheDefaultConfTwoFlowFilesNoKey() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();

        properties1.clear();
        getRunner.enqueue("".getBytes(),properties1);
        getRunner.enqueue("".getBytes(),properties1);

        getIgniteCache.initialize(getRunner.getProcessContext());

        getRunner.run(2, false, true);

        getRunner.assertAllFlowFilesTransferred(GetIgniteCache.REL_FAILURE, 2);
        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(0, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(2, failureFlowFiles.size());

        final MockFlowFile out1 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);
        out1.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);
        final MockFlowFile out2 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(1);
        out2.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);

        getRunner.shutdown();

    }

    @Test
    public void testGetIgniteCacheDefaultConfTwoFlowFileFirstNoKey() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.CACHE_NAME, CACHE_NAME);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        getRunner.enqueue("".getBytes());
        getRunner.enqueue("".getBytes(),properties2);
        getIgniteCache.initialize(getRunner.getProcessContext());
        getIgniteCache.getIgniteCache().put("key2", "test2".getBytes());

        getRunner.run(2, false, true);

        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(1, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(1, failureFlowFiles.size());

        final MockFlowFile out1 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);

        out1.assertContentEquals("".getBytes());
        out1.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);

        final MockFlowFile out2 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);

        out2.assertContentEquals("test2".getBytes());
        assertArrayEquals("test2".getBytes(),(byte[])getIgniteCache.getIgniteCache().get("key2"));

        getRunner.shutdown();
    }

    @Test
    public void testGetIgniteCacheDefaultConfTwoFlowFileSecondNoKey() throws IOException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.CACHE_NAME, CACHE_NAME);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        getRunner.enqueue("".getBytes(),properties1);
        getRunner.enqueue("".getBytes());
        getIgniteCache.initialize(getRunner.getProcessContext());

        getIgniteCache.getIgniteCache().put("key1", "test1".getBytes());
        getRunner.run(2, false, true);

        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(1, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(1, failureFlowFiles.size());

        final MockFlowFile out1 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);

        out1.assertContentEquals("".getBytes());
        out1.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);

        final MockFlowFile out2 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);

        out2.assertContentEquals("test1".getBytes());
        assertArrayEquals("test1".getBytes(),(byte[])getIgniteCache.getIgniteCache().get("key1"));

        getRunner.shutdown();

    }


    @Test
    public void testGetIgniteCacheDefaultConfThreeFlowFilesOneOkSecondOkThirdNoExpressionKey() throws IOException, InterruptedException {
        getRunner = TestRunners.newTestRunner(getIgniteCache);
        getRunner.setProperty(GetIgniteCache.CACHE_NAME, CACHE_NAME);
        getRunner.setProperty(GetIgniteCache.IGNITE_CACHE_ENTRY_KEY, "${igniteKey}");

        getRunner.assertValid();
        getRunner.enqueue("".getBytes(),properties1);
        getRunner.enqueue("".getBytes(),properties2);
        getRunner.enqueue("".getBytes());
        getIgniteCache.initialize(getRunner.getProcessContext());

        getIgniteCache.getIgniteCache().put("key1", "test1".getBytes());
        getIgniteCache.getIgniteCache().put("key2", "test2".getBytes());
        getRunner.run(3, false, true);

        List<MockFlowFile> sucessfulFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS);
        assertEquals(2, sucessfulFlowFiles.size());
        List<MockFlowFile> failureFlowFiles = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE);
        assertEquals(1, failureFlowFiles.size());

        final MockFlowFile out1 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_FAILURE).get(0);

        out1.assertContentEquals("".getBytes());
        out1.assertAttributeEquals(GetIgniteCache.IGNITE_GET_FAILED_REASON_ATTRIBUTE_KEY, GetIgniteCache.IGNITE_GET_FAILED_MISSING_KEY_MESSAGE);

        final MockFlowFile out2 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(0);

        out2.assertContentEquals("test1".getBytes());
        assertArrayEquals("test1".getBytes(),(byte[])getIgniteCache.getIgniteCache().get("key1"));

        final MockFlowFile out3 = getRunner.getFlowFilesForRelationship(GetIgniteCache.REL_SUCCESS).get(1);

        out3.assertContentEquals("test2".getBytes());
        assertArrayEquals("test2".getBytes(),(byte[])getIgniteCache.getIgniteCache().get("key2"));

        getRunner.shutdown();

    }

}
