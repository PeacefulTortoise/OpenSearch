/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client;

import org.elasticsearch.client.http.HttpEntity;
import org.elasticsearch.client.http.entity.BufferedHttpEntity;
import org.elasticsearch.client.http.util.EntityUtils;

import java.io.IOException;

/**
 * Exception thrown when an elasticsearch node responds to a request with a status code that indicates an error.
 * Holds the response that was returned.
 */
public final class ResponseException extends IOException {

    private Response response;

    public ResponseException(Response response) throws IOException {
        super(buildMessage(response));
        this.response = response;
    }

    private static String buildMessage(Response response) throws IOException {
        String message = response.getRequestLine().getMethod() + " " + response.getHost() + response.getRequestLine().getUri()
                + ": " + response.getStatusLine().toString();

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            if (entity.isRepeatable() == false) {
                entity = new BufferedHttpEntity(entity);
                response.getHttpResponse().setEntity(entity);
            }
            message += "\n" + EntityUtils.toString(entity);
        }
        return message;
    }

    /**
     * Returns the {@link Response} that caused this exception to be thrown.
     */
    public Response getResponse() {
        return response;
    }
}
