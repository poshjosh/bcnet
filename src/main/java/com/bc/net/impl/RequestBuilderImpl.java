/*
 * Copyright 2018 NUROX Ltd.
 *
 * Licensed under the NUROX Ltd Software License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.looseboxes.com/legal/licenses/software.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bc.net.impl;

import com.bc.net.CookieProcessor;
import com.bc.net.RequestBodyWriter;
import com.bc.net.Response;
import com.bc.net.util.UserAgents;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import com.bc.net.RequestBuilder;
import java.util.Objects;
import java.util.logging.Level;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 6:46:25 PM
 */
public class RequestBuilderImpl implements Serializable, RequestBuilder {

    private transient static final Logger LOG = Logger.getLogger(RequestBuilderImpl.class.getName());

    private boolean generateRandomUserAgent;
    private boolean mobileUserAgent;

    private int connectTimeout;
    private int readTimeout;
    private int chunkedStreamingBuffer;
    private int fixedLengthStreamingBuffer;

    private URL url; 
    
    private String method;
    
    private boolean readyToPopulate;
    
    private boolean followRedirects;

    private CookieProcessor cookieProcessor;

    private final Set<String> cookies;
    private final Map<String, Object> requestProperties; 
    private final RequestBodyWriter<RequestBuilder> bodyBuilder;

    public RequestBuilderImpl() {
        this.requestProperties = new HashMap<>();
        this.cookies = new LinkedHashSet<>();
        this.bodyBuilder = new RequestBodyWriterImpl<>(this);
        this.reset();
    }
    
    @Override
    public RequestBuilder reset() {
        this.cookieProcessor = new CookieProcessorImpl();
        this.followRedirects = true;
        this.generateRandomUserAgent = true;
        this.mobileUserAgent = false;
        this.connectTimeout = -1;
        this.readTimeout = -1;
        final boolean settingChunkedStreamingTo8192OftenCausesErrorCode400 = true;
        if(!settingChunkedStreamingTo8192OftenCausesErrorCode400) {
            this.chunkedStreamingBuffer = 8192;
        }
        this.fixedLengthStreamingBuffer = 0;
        this.bodyBuilder.reset();
        this.clear();
        return this;
    }
    
    public void clear() {
        this.url = null;
//        this.cookies.clear();
        this.requestProperties.clear();
        this.readyToPopulate = true;
    }
    
    @Override
    public RequestBuilder clearCookies() {
        this.cookies.clear();
        return this;
    }
    
    public void makeReadyToPopulate() {
        if(!this.readyToPopulate) {
            this.clear();
        }
    }
    
    @Override
    public RequestBodyWriter<RequestBuilder> body() {
        return this.bodyBuilder;
    }

    @Override
    public Response response() throws IOException {
        final URLConnection connection = this.build();
        final Response response = new ResponseImpl(connection, this.cookieProcessor);
        this.cookies.addAll(response.getCookies());
        return response;
    }

    @Override
    public URLConnection build() throws IOException{
        
        this.readyToPopulate = false;
        
        final long mb4 = this.availableMemory();
        final long tb4 = System.currentTimeMillis();
        
        Objects.requireNonNull(this.cookieProcessor);
        
        final URLConnection connection = url.openConnection();
        
        LOG.finer(() -> "Opened connection. Spent, time: " + (System.currentTimeMillis() - tb4) + 
                ", memory: " + this.usedMemory(mb4) + ", connection: " + connection);

        connection.setDoOutput(this.bodyBuilder.hasOutput());
        connection.setDoInput(true);
        
        LOG.finer(() -> "Streaming FixedLength: " + this.fixedLengthStreamingBuffer + 
                ", Chunked: " + this.chunkedStreamingBuffer);
        
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            if (this.getFixedLengthStreamingBuffer() > 0) {
                (httpConn).setFixedLengthStreamingMode(this.getFixedLengthStreamingBuffer());
            } else if (this.getChunkedStreamingBuffer() > 0) {
                (httpConn).setChunkedStreamingMode(this.getChunkedStreamingBuffer());
            }
            if (readTimeout > -1) {
                httpConn.setReadTimeout(readTimeout);
            }
            if (connectTimeout > -1) {
                httpConn.setConnectTimeout(connectTimeout);
            }
            httpConn.setInstanceFollowRedirects(followRedirects);
        }
        
        if (this.generateRandomUserAgent) {
            this.addRandomUserAgentFor(url);
        }
        
        this.populateConnection(connection);
        
        final Level level = cookies == null || cookies.isEmpty() ? Level.FINER :Level.FINE;
        LOG.log(level, () -> "Adding cookies to request: " + cookies);            
        this.cookieProcessor.addCookiesToRequest(connection, cookies);
        
        if(method != null) {
            if(connection instanceof HttpURLConnection) {
                ((HttpURLConnection)connection).setRequestMethod(method);  
            }
        }
        
        if(this.bodyBuilder.hasOutput()) {
            this.bodyBuilder.write(connection);
        }

        return connection;
    }

    protected void populateConnection(URLConnection connection) {
        
        final long mb4 = this.availableMemory();
        final long tb4 = System.currentTimeMillis();
        
        if (requestProperties != null) {
            
            for (String key : requestProperties.keySet()) {
                Object val = requestProperties.get(key);
                if (val != null) {
                    
                    LOG.finer(() -> "Settting request property: " + key + '=' + val);
                    
                    connection.setRequestProperty(key, val.toString());
                }
            }
        }
        
        LOG.finer(() -> "Populated connection with request properties. Spent, time: " + 
                (System.currentTimeMillis() - tb4) + ", memory: " + this.usedMemory(mb4));
    }

    public void addRandomUserAgentFor(URL url) {
        Object userAgent = requestProperties.get("User-Agent");
        if (userAgent == null) {
            userAgent = requestProperties.get("user-agent");
            if (userAgent == null) {
                userAgent = url == null ? new UserAgents().getAny(this.mobileUserAgent) : 
                        new UserAgents().getAny(url, this.mobileUserAgent);
                this.userAgent(userAgent.toString());
            }
        }
    }

    @Override
    public RequestBuilder add(String key, Object value) {
        this.makeReadyToPopulate();
        this.requestProperties.put(key, value);
        return this;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    @Override
    public RequestBuilder followRedirects(boolean follow) {
        this.makeReadyToPopulate();
        this.followRedirects = follow;
        return this;
    }

    @Override
    public CookieProcessor getCookieProcessor() {
        return cookieProcessor;
    }

    @Override
    public RequestBuilder cookieProcessor(CookieProcessor cookieProcessor) {
        this.makeReadyToPopulate();
        this.cookieProcessor = cookieProcessor;
        return this;
    }
    
    @Override
    public URL getUrl() {
        return url;
    }
    
    @Override
    public RequestBuilder url(URL url) {
        this.makeReadyToPopulate();
        this.url = url;
        return this;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public RequestBuilder method(String method) {
        this.makeReadyToPopulate();
        this.method = method;
        return this;
    }

    @Override
    public int getChunkedStreamingBuffer() {
        return chunkedStreamingBuffer;
    }

    @Override
    public RequestBuilderImpl chunkedStreamingBuffer(int chunkedStreamingBuffer) {
        this.makeReadyToPopulate();
        this.chunkedStreamingBuffer = chunkedStreamingBuffer;
        return this;
    }

    @Override
    public int getFixedLengthStreamingBuffer() {
        return fixedLengthStreamingBuffer;
    }

    @Override
    public RequestBuilderImpl fixedLengthStreamingBuffer(int fixedLengthStreamingBuffer) {
        this.makeReadyToPopulate();
        this.fixedLengthStreamingBuffer = fixedLengthStreamingBuffer;
        return this;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public RequestBuilderImpl connectTimeout(int connectTimeout) {
        this.makeReadyToPopulate();
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public RequestBuilderImpl readTimeout(int readTimeout) {
        this.makeReadyToPopulate();
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public boolean isGenerateRandomUserAgent() {
        return generateRandomUserAgent;
    }

    @Override
    public RequestBuilder randomUserAgent(boolean yes) {
        this.makeReadyToPopulate();
        this.generateRandomUserAgent = yes;
        return this;
    }

    @Override
    public boolean isMobileUserAgent() {
        return this.mobileUserAgent;
    }

    @Override
    public RequestBuilder mobileUserAgent(boolean mobile) {
        this.makeReadyToPopulate();
        this.mobileUserAgent = mobile;
        return this;
    }

    @Override
    public List<String> getCookies() {
        return this.cookies.isEmpty() ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList(cookies));
    }

    @Override
    public RequestBuilderImpl addCookies(Collection<String> cookiesToAdd) {
        this.makeReadyToPopulate();
        this.cookies.addAll(cookiesToAdd);
        return this;
    }

    @Override
    public Map<String, Object> getRequestProperties() {
        return Collections.unmodifiableMap(requestProperties);
    }

    private transient static final Runtime runtime = Runtime.getRuntime();
    
    public final long usedMemory(long bookmarkMemory) {
        return bookmarkMemory - availableMemory();
    }
    
    public final long availableMemory() {
        final long totalMemory = runtime.totalMemory(); // current heap allocated to the VM process
        final long freeMemory = runtime.freeMemory(); // out of the current heap, how much is free
        final long maxMemory = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        final long usedMemory = totalMemory - freeMemory; // how much of the current heap the VM is using
        final long availableMemory = maxMemory - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
        return availableMemory;
    }  
}
