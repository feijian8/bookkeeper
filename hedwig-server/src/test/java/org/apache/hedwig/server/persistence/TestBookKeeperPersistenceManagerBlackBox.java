/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hedwig.server.persistence;

import java.util.concurrent.Executors;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.After;
import org.junit.Before;

import org.apache.bookkeeper.util.OrderedSafeExecutor;
import org.apache.hedwig.server.common.ServerConfiguration;
import org.apache.hedwig.server.meta.MetadataManagerFactory;
import org.apache.hedwig.server.topics.TrivialOwnAllTopicManager;

public class TestBookKeeperPersistenceManagerBlackBox extends TestPersistenceManagerBlackBox {
    BookKeeperTestBase bktb;
    private final int numBookies = 3;

    MetadataManagerFactory metadataManagerFactory = null;

    @Override
    @Before
    protected void setUp() throws Exception {
        // We need to setUp this class first since the super.setUp() method will
        // need the BookKeeperTestBase to be instantiated.
        bktb = new BookKeeperTestBase(numBookies);
        bktb.setUp();
        super.setUp();
    }

    @Override
    @After
    protected void tearDown() throws Exception {
        bktb.tearDown();
        super.tearDown();
        if (null != metadataManagerFactory) {
            metadataManagerFactory.shutdown();
        }
    }

    @Override
    long getLowestSeqId() {
        return 1;
    }

    @Override
    PersistenceManager instantiatePersistenceManager() throws Exception {
        ServerConfiguration conf = new ServerConfiguration();

        OrderedSafeExecutor scheduler = new OrderedSafeExecutor(conf.getNumSharedQueuerThreads());

        metadataManagerFactory =
            MetadataManagerFactory.newMetadataManagerFactory(conf, bktb.getZooKeeperClient());

        return new BookkeeperPersistenceManager(bktb.bk, metadataManagerFactory,
                                                new TrivialOwnAllTopicManager(conf, scheduler),
                                                conf, scheduler);
    }

    @Override
    public long getExpectedSeqId(int numPublished) {
        return numPublished;
    }

    public static Test suite() {
        return new TestSuite(TestBookKeeperPersistenceManagerBlackBox.class);
    }

}
