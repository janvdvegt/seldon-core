/*******************************************************************************
 * Copyright 2017 Seldon Technologies Ltd (http://www.seldon.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.seldon.clustermanager.pb;

import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import io.kubernetes.client.proto.Meta.Time;

public class TimeFormatTest {

    @Test
    public void parseTimestamp() throws InvalidProtocolBufferException
    {
        String json = "\"2017-11-23T20:37:27Z\"";
        Time.Builder builder = Time.newBuilder();
        JsonFormat.parser().usingTypeParser(Time.getDescriptor().getFullName(),new TimeUtils.TimeParser()).merge(json, builder);
    }
    
}
