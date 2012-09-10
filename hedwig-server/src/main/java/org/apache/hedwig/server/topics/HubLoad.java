/*
 *
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
 *
 */
package org.apache.hedwig.server.topics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.hedwig.protocol.PubSubProtocol.HubLoadData;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

/**
 * This class encapsulates metrics for determining the load on a hub server.
 */
public class HubLoad implements Comparable<HubLoad> {

    public static final HubLoad MAX_LOAD = new HubLoad(Long.MAX_VALUE);
    public static final HubLoad MIN_LOAD = new HubLoad(0);

    public static class InvalidHubLoadException extends Exception {
        public InvalidHubLoadException(String msg) {
            super(msg);
        }

        public InvalidHubLoadException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    // how many topics that a hub server serves
    long numTopics;

    public HubLoad(long num) {
        this.numTopics = num;
    }

    public HubLoad(HubLoadData data) {
        this.numTopics = data.getNumTopics();
    }

    // TODO: Make this threadsafe (BOOKKEEPER-379)
    public HubLoad setNumTopics(long numTopics) {
        this.numTopics = numTopics;
        return this;
    }

    public HubLoadData toHubLoadData() {
        return HubLoadData.newBuilder().setNumTopics(numTopics).build();
    }

    @Override
    public String toString() {
        return TextFormat.printToString(toHubLoadData());
    }

    @Override
    public boolean equals(Object o) {
        if (null == o ||
            !(o instanceof HubLoad)) {
            return false;
        }
        return 0 == compareTo((HubLoad)o);
    }

    @Override
    public int compareTo(HubLoad other) {
        return numTopics > other.numTopics ?
               1 : (numTopics < other.numTopics ? -1 : 0);
    }

    @Override
    public int hashCode() {
        return (int)numTopics;
    }

    /**
     * Parse hub load from a string.
     *
     * @param hubLoadStr
     *          String representation of hub load
     * @return hub load
     * @throws InvalidHubLoadException when <code>hubLoadStr</code> is not a valid
     *         string representation of hub load.
     */
    public static HubLoad parse(String hubLoadStr) throws InvalidHubLoadException {
        // it is no protobuf encoded hub info, it might be generated by ZkTopicManager
        if (!hubLoadStr.startsWith("numTopics")) {
            try {
                long numTopics = Long.parseLong(hubLoadStr, 10);
                return new HubLoad(numTopics);
            } catch (NumberFormatException nfe) {
                throw new InvalidHubLoadException("Corrupted hub load data : " + hubLoadStr, nfe);
            }
        }
        // it it a protobuf encoded hub load data.
        HubLoadData hubLoadData;
        try {
            BufferedReader reader = new BufferedReader(
                new StringReader(hubLoadStr));
            HubLoadData.Builder dataBuilder = HubLoadData.newBuilder();
            TextFormat.merge(reader, dataBuilder);
            hubLoadData = dataBuilder.build();
        } catch (InvalidProtocolBufferException ipbe) {
            throw new InvalidHubLoadException("Corrupted hub load data : " + hubLoadStr, ipbe);
        } catch (IOException ie) {
            throw new InvalidHubLoadException("Corrupted hub load data : " + hubLoadStr, ie);
        }

        return new HubLoad(hubLoadData);
    }
}