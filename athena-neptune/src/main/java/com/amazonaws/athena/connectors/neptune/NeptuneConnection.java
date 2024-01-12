/*-
 * #%L
 * athena-neptune
 * %%
 * Copyright (C) 2019 - 2020 Amazon Web Services
 * %%
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
 * #L%
 */
package com.amazonaws.athena.connectors.neptune;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.neptune.auth.NeptuneNettyHttpSigV4Signer;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class NeptuneConnection
{
    private static Cluster cluster = null;
    
    private String neptuneEndpoint;
    private String neptunePort;
    private boolean enabledIAM;
    private String region;

    protected NeptuneConnection(String neptuneEndpoint, String neptunePort, boolean enabledIAM, String region) 
    {
        Cluster.Builder builder = Cluster.build();
        builder.addContactPoint(neptuneEndpoint)
               .port(Integer.parseInt(neptunePort))
               .enableSsl(true);
               
        if (enabledIAM) {
            builder.handshakeInterceptor(r ->
                    {
                        try {
                            NeptuneNettyHttpSigV4Signer sigV4Signer =
                                    new NeptuneNettyHttpSigV4Signer(region, new DefaultAWSCredentialsProviderChain());
                            sigV4Signer.signRequest(r);
                        }
                        catch (NeptuneSigV4SignerException e) {
                            throw new RuntimeException("Exception occurred while signing the request", e);
                        }
                        return r;
                    }
            );
        }
        
        cluster = builder.create();
        this.neptuneEndpoint = neptuneEndpoint;
        this.neptunePort = neptunePort;
        this.enabledIAM = enabledIAM;
        this.region = region;
    }
    
    public String getNeptuneEndpoint()
    {
        return this.neptuneEndpoint;
    }

    public String getNeptunePort()
    {
        return this.neptunePort;
    }
    
    public boolean isEnabledIAM() 
    {
        return this.enabledIAM;
    }

    public String getRegion()
    {
        return this.region;
    }

    public Client getNeptuneClientConnection()
    {
        return cluster.connect();
    }

    public GraphTraversalSource getTraversalSource(Client client)
    {
        DriverRemoteConnection connection = DriverRemoteConnection.using(client);
        return AnonymousTraversalSource.traversal().withRemote(connection);
    }

    void closeCluster()
    {
        cluster.close();
    }
}
