/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.hadoop.hive;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.HiveMetaHook;
import org.apache.hadoop.hive.ql.metadata.DefaultStorageHandler;
import org.apache.hadoop.hive.ql.plan.TableDesc;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;
import org.elasticsearch.hadoop.mr.ESConfigConstants;
import org.elasticsearch.hadoop.mr.ESInputFormat;
import org.elasticsearch.hadoop.mr.ESOutputFormat;
import org.elasticsearch.hadoop.util.ConfigUtils;

/**
 * Hive storage for writing data into an ElasticSearch index.
 *
 * The ElasticSearch host/port can be specified through Hadoop properties (see package description)
 * or passed to {@link #ESStorageHandler} through Hive <tt>TBLPROPERTIES</tt>
 */
public class ESStorageHandler extends DefaultStorageHandler {

    private String host;
    private int port = 0;

    @Override
    public Class<? extends InputFormat> getInputFormatClass() {
        return ESInputFormat.class;
    }

    @Override
    public Class<? extends OutputFormat> getOutputFormatClass() {
        return ESHiveOutputFormat.class;
    }

    @Override
    public Class<? extends SerDe> getSerDeClass() {
        return ESSerDe.class;
    }

    @Override
    public HiveMetaHook getMetaHook() {
        //TODO: add metahook support
        return null;
    }

    @Override
    public void configureInputJobProperties(TableDesc tableDesc, Map<String, String> jobProperties) {
        init(tableDesc);
    }

    @Override
    public void configureOutputJobProperties(TableDesc tableDesc, Map<String, String> jobProperties) {
        init(tableDesc);
    }

    private void init(TableDesc tableDesc) {
        Properties properties = tableDesc.getProperties();

        host = properties.getProperty(ESConfigConstants.ES_HOST);
        port = Integer.valueOf(properties.getProperty(ESConfigConstants.ES_PORT, "0"));
        String location = properties.getProperty(ESConfigConstants.ES_LOCATION);

        if (StringUtils.isBlank(location)) {
            throw new IllegalArgumentException("No location specified" + location);
        }
        Configuration cfg = getConf();

        cfg.set(ESConfigConstants.ES_ADDRESS, ConfigUtils.detectHostPortAddress(host, port, cfg));
        cfg.set(ESConfigConstants.ES_QUERY, location.trim());
        cfg.set(ESConfigConstants.ES_INDEX, location.trim());

        // replace the default committer when using the old API
        cfg.set("mapred.output.committer.class", ESOutputFormat.ESOutputCommitter.class.getName());
    }

    @Override
    @Deprecated
    public void configureTableJobProperties(TableDesc tableDesc, Map<String, String> jobProperties) {
        throw new UnsupportedOperationException();
    }
}