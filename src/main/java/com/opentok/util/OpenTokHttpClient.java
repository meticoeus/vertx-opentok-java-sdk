/**
 * OpenTok Java SDK
 * Copyright (C) 2018 TokBox, Inc.
 * http://www.tokbox.com
 * <p>
 * Licensed under The MIT License (MIT). See LICENSE file for more information.
 */
package com.opentok.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opentok.ArchiveProperties;
import com.opentok.constants.DefaultApiUrl;
import com.opentok.constants.Version;
import com.opentok.exception.OpenTokException;
import com.opentok.exception.RequestException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

import java.util.*;
import java.util.Map.Entry;

// TODO: add ,t) to all exceptions
// TODO: check all requests against the original data to nsure we are sending the same method (and body)
// TODO: beging converting OpenTok
public class OpenTokHttpClient {

    private final String apiUrl;
    private final String apiSecret;
    private final String authHeader = "X-OPENTOK-AUTH";
    private final int apiKey;
    private final Vertx vertx;
    private final HttpClient httpClient;
    private String userAgent;

    private OpenTokHttpClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.apiUrl = builder.apiUrl;
        this.apiSecret = builder.apiSecret;
        this.vertx = builder.vertx;
        this.httpClient = builder.httpClient;
    }

    public void createSession(Map<String, Collection<String>> params, Handler<AsyncResult<String>> handler) {
        try {
            String url = this.apiUrl + "/session/create";
            Map<String, List<String>> paramsWithList = null;
            if (params != null) {
                paramsWithList = new HashMap<>();
                for (Entry<String, Collection<String>> entry : params.entrySet()) {
                    paramsWithList.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }

            HttpClientRequest request = this.httpClient.postAbs(url, response -> {
                try {
                    response.exceptionHandler(t ->
                            handler.handle(Future.failedFuture(new RequestException("Could not create an OpenTok Session", t)))
                    );

                    response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
                } catch (Throwable t) {
                    handler.handle(Future.failedFuture(new RequestException("Could not create an OpenTok Session", t)));
                }
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not create an OpenTok Session", t)))
            );

            setAuthHeaders(request, handler)
                    .putHeader("Accept", "application/json")
                    .end(RequestUtils.buildBodyFromParams(paramsWithList));
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not create an OpenTok Session", t)));
        }
    }

    public void getArchive(String archiveId, Handler<AsyncResult<String>> handler) {
        try {
            String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId;

            HttpClientRequest request = this.httpClient.getAbs(url, response -> {
                try {
                    response.exceptionHandler(t -> {
                        Throwable error;
                        switch (response.statusCode()) {
                            case 400:
                                error = new RequestException("Could not get an OpenTok Archive. The archiveId was invalid. " +
                                        "archiveId: " + archiveId, t);
                                break;

                            case 403:
                                error = new RequestException("Could not get an OpenTok Archive. The request was not authorized.", t);
                                break;

                            case 500:
                                error = new RequestException("Could not get an OpenTok Archive. A server error occurred.", t);
                                break;

                            default:
                                error = new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                                        " response code: " + response.statusCode(), t);
                        }

                        handler.handle(Future.failedFuture(error));
                    });

                    response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
                } catch (Throwable t) {
                    handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive. The server response was invalid.", t)));
                }
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive. The server response was invalid.", t)))
            );

            setAuthHeaders(request, handler)
                    .end();
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive.", t)));
        }
    }

    public void getArchives(int offset, int count, Handler<AsyncResult<String>> handler) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive";
        if (offset != 0 || count != 1000) {
            url += "?";
            if (offset != 0) {
                url += ("offset=" + Integer.toString(offset) + '&');
            }
            if (count != 1000) {
                url += ("count=" + Integer.toString(count));
            }
        }

        getArchivesImpl(url, handler);
    }

    public void getArchives(String sessionId, Handler<AsyncResult<String>> handler) {
        String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive?sessionId=" + sessionId;

        getArchivesImpl(url, handler);
    }

    private void getArchivesImpl(String url, Handler<AsyncResult<String>> handler) {
        try {
            HttpClientRequest request = this.httpClient.getAbs(url, response -> {
                response.exceptionHandler(t -> {
                    Throwable error;
                    switch (response.statusCode()) {
                        case 403:
                            error = new RequestException("Could not get OpenTok Archives. The request was not authorized.", t);
                            break;

                        case 500:
                            error = new RequestException("Could not get OpenTok Archives. A server error occurred.", t);
                            break;

                        default:
                            error = new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                                    " response code: " + response.statusCode(), t);
                    }

                    handler.handle(Future.failedFuture(error));
                });

                response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive. The server response was invalid.", t)))
            );

            setAuthHeaders(request, handler)
                    .end();
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive.", t)));
        }
    }

    public void startArchive(String sessionId, ArchiveProperties properties, Handler<AsyncResult<String>> handler) {
        try {
            String requestBody = null;
            String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive";

            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode requestJson = nodeFactory.objectNode();
            requestJson.put("sessionId", sessionId);
            requestJson.put("hasVideo", properties.hasVideo());
            requestJson.put("hasAudio", properties.hasAudio());
            requestJson.put("outputMode", properties.outputMode().toString());
            if (properties.layout() != null) {
                ObjectNode layout = requestJson.putObject("layout");
                layout.put("type", properties.layout().getType().toString());
                layout.put("stylesheet", properties.layout().getStylesheet());
            }
            if (properties.name() != null) {
                requestJson.put("name", properties.name());
            }
            try {
                requestBody = new ObjectMapper().writeValueAsString(requestJson);
            } catch (JsonProcessingException e) {
                handler.handle(Future.failedFuture(new OpenTokException("Could not start an OpenTok Archive. The JSON body encoding failed.", e)));
            }

            HttpClientRequest request = this.httpClient.postAbs(url, response -> {
                try {
                    response.exceptionHandler(t -> {
                        Throwable error;
                        switch (response.statusCode()) {
                            case 403:
                                error = new RequestException("Could not start an OpenTok Archive. The request was not authorized.", t);
                                break;

                            case 404:
                                error = new RequestException("Could not start an OpenTok Archive. The sessionId does not exist. " +
                                        "sessionId = " + sessionId, t);
                                break;

                            case 409:
                                error = new RequestException("Could not start an OpenTok Archive. The session is either " +
                                        "peer-to-peer or already recording. sessionId = " + sessionId, t);
                                break;

                            case 500:
                                error = new RequestException("Could not start an OpenTok Archive. A server error occurred.", t);
                                break;

                            default:
                                error = new RequestException("Could not start an OpenTok Archive. The server response was invalid." +
                                        " response code: " + response.statusCode(), t);
                        }

                        handler.handle(Future.failedFuture(error));
                    });

                    response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
                } catch (Throwable t) {
                    handler.handle(Future.failedFuture(new RequestException("Could not start an OpenTok Archive. The server response was invalid.", t)));
                }
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not start an OpenTok Archive. The server response was invalid.", t)))
            );

            setAuthHeaders(request, handler)
                    .putHeader("Accept", "application/json")
                    .end(requestBody);
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not start an OpenTok Archive.", t)));
        }
    }

    public void stopArchive(String archiveId, Handler<AsyncResult<String>> handler) {
        try {
            String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId + "/stop";

            HttpClientRequest request = this.httpClient.postAbs(url, response -> {
                try {
                    response.exceptionHandler(t -> {
                        Throwable error;
                        switch (response.statusCode()) {
                            case 400:
                                // NOTE: the REST api spec talks about sessionId and action, both of which aren't required.
                                //       see: https://github.com/opentok/OpenTok-2.0-archiving-samples/blob/master/REST-API.md#stop_archive
                                error = new RequestException("Could not stop an OpenTok Archive.", t);
                                break;

                            case 403:
                                error = new RequestException("Could not stop an OpenTok Archive. The request was not authorized.", t);
                                break;

                            case 404:
                                error = new RequestException("Could not stop an OpenTok Archive. The archiveId does not exist. " +
                                        "archiveId = " + archiveId, t);
                                break;

                            case 409:
                                error = new RequestException("Could not stop an OpenTok Archive. The archive is not being recorded. " +
                                        "archiveId = " + archiveId, t);
                                break;

                            case 500:
                                error = new RequestException("Could not stop an OpenTok Archive. A server error occurred.", t);
                                break;

                            default:
                                error = new RequestException("Could not stop an OpenTok Archive. The server response was invalid." +
                                        " response code: " + response.statusCode(), t);
                        }

                        handler.handle(Future.failedFuture(error));
                    });

                    response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
                } catch (Throwable t) {
                    handler.handle(Future.failedFuture(new RequestException("Could not stop an OpenTok Archive. The server response was invalid.", t)));
                }
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not stop an OpenTok Archive. The server response was invalid.", t)))
            );

            setAuthHeaders(request, handler)
                    .end();
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not stop an OpenTok Archive.", t)));
        }
    }

    public void deleteArchive(String archiveId, Handler<AsyncResult<String>> handler) {
        try {
            String url = this.apiUrl + "/v2/project/" + this.apiKey + "/archive/" + archiveId;

            HttpClientRequest request = this.httpClient.deleteAbs(url, response -> {
                try {
                    response.exceptionHandler(t -> {
                        Throwable error;
                        switch (response.statusCode()) {
                            case 403:
                                error = new RequestException("Could not delete an OpenTok Archive. The request was not authorized.", t);
                                break;

                            case 409:
                                error = new RequestException("Could not delete an OpenTok Archive. The status was not \"uploaded\"," +
                                        " \"available\", or \"deleted\". archiveId = " + archiveId, t);
                                break;

                            case 500:
                                error = new RequestException("Could not delete an OpenTok Archive. A server error occurred.", t);
                                break;

                            default:
                                error = new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                                        " response code: " + response.statusCode(), t);
                        }

                        handler.handle(Future.failedFuture(error));
                    });

                    response.bodyHandler(buffer -> handler.handle(Future.succeededFuture(buffer.toString())));
                } catch (Throwable t) {
                    handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive. The server response was invalid.", t)));
                }
            });

            request.exceptionHandler(t ->
                    handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive. The server response was invalid.", t)))
            );

            setAuthHeaders(request, handler)
                    .end();
        } catch (Throwable t) {
            handler.handle(Future.failedFuture(new RequestException("Could not get an OpenTok Archive.", t)));
        }
    }

    public void close() {
        this.httpClient.close();
    }

    public static class Builder {
        private final int apiKey;
        private final String apiSecret;
        private Vertx vertx;
        private HttpClientOptions httpClientOptions;
        private HttpClient httpClient;
        private String apiUrl;

        public Builder(int apiKey, String apiSecret, Vertx vertx) {
            this.vertx = vertx;
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder httpClientOptions(HttpClientOptions httpClientOptions) {
            this.httpClientOptions = httpClientOptions;
            return this;
        }

        public OpenTokHttpClient build() {
            if (this.apiUrl == null) {
                this.apiUrl = DefaultApiUrl.DEFAULT_API_URI;
            }
            if (this.httpClientOptions == null) {
                this.httpClient = vertx.createHttpClient();
            } else {
                this.httpClient = vertx.createHttpClient(this.httpClientOptions);
            }

            // NOTE: not thread-safe, config could be modified by another thread here?
            OpenTokHttpClient client = new OpenTokHttpClient(this);
            return client;
        }
    }

    private <T> HttpClientRequest setAuthHeaders(HttpClientRequest request, Handler<AsyncResult<T>> handler) {
        request.putHeader("User-Agent", this.getUserAgent());
        try {
            request.putHeader(authHeader, TokenGenerator.generateToken(apiKey, apiSecret));
        } catch (OpenTokException e) {
            handler.handle(Future.failedFuture(e));
        }
        return request;
    }

    private String getUserAgent() {
        if (this.userAgent == null) {
            this.userAgent = "Opentok-Java-SDK/" + Version.VERSION + " JRE/" + System.getProperty("java.version");
        }

        return this.userAgent;
    }
}
