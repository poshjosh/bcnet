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
import com.bc.net.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 9:45:00 AM
 */
public class ResponseImpl implements Response{

    private transient static final Logger LOG = Logger.getLogger(ResponseImpl.class.getName());
    
    private final URLConnection urlConnection;

    private final InputStream inputStream;
    
    private final List<String> cookies;
    
    private int responseCode;
    
    private String responseMessage;
    
    private CookieProcessor cookieProcessor;

    public ResponseImpl(URLConnection urlConn) throws IOException {
        this(urlConn, CookieProcessor.NO_COOKIES);
    }
    
    public ResponseImpl(URLConnection urlConn, CookieProcessor cookieProcessor) throws IOException {
        this.urlConnection = Objects.requireNonNull(urlConn);
        this.inputStream = this.getInputStream(urlConn);
        this.cookieProcessor = Objects.requireNonNull(cookieProcessor);
        if(urlConn instanceof HttpURLConnection) {
            final HttpURLConnection httpConn = ((HttpURLConnection)urlConn);
            this.responseCode = this.getResponseCode(httpConn, -1);
            this.responseMessage = this.getResponseMessage(httpConn, null);
        }
        this.cookies = this.cookieProcessor.loadCookiesFromResponse(urlConn);
        LOG.fine(() -> "Loaded cookies from response: " + this.cookies);            
    }
    
    public InputStream getInputStream(URLConnection urlConn) throws IOException {

//final long mb4 = com.bc.util.Util.availableMemory();        
//final long tb4 = System.currentTimeMillis();  
        
        int code = -1;
        HttpURLConnection httpConn = null;
        if(urlConn instanceof HttpURLConnection) {
            httpConn = (HttpURLConnection)urlConn;
            code = this.getResponseCode(httpConn, -1);
        }

// http://www.oracle.com/technetwork/java/javase/compatibility-417013.html
// At the page search for: Invalid Http Response .. for possiblity of -1 response code

        InputStream in = null;
        
        if(code == -1 || code >= 400) {

            if(httpConn != null) {

                in = httpConn.getErrorStream();
            }

            if(in == null) {

                in = urlConn.getInputStream();
            }
        }else{

            in = urlConn.getInputStream();

            if(in == null && httpConn != null) {

                in = httpConn.getErrorStream();
            }
        }
        
        if(in != null) {
            
            final String s = urlConn.getContentEncoding();
            final String contentEncoding = s == null ? null : s.toLowerCase();
            
            if (null != contentEncoding && contentEncoding.contains("gzip")) {
                
                in = new GZIPInputStream (in);
                
            } else if (null != contentEncoding && contentEncoding.contains("deflate")){
                
                in = new InflaterInputStream(in, new Inflater(true));
            }
        }

//log(Level.FINER, 
//"Done getting input stream. Spent, time: {0}, memory: {1}", 
//System.currentTimeMillis()-tb4, com.bc.util.Util.usedMemory(mb4));

        return Objects.requireNonNull(in);
    }
    
    public int getResponseCode(HttpURLConnection httpConn, int outputIfNone) {
        try{
            return httpConn.getResponseCode();
        }catch(IOException e) {
            LOG.log(Level.WARNING, "Failed to retrieve response code", e);        
            return outputIfNone;
        }
    }
    
    public String getResponseMessage(HttpURLConnection httpConn, String outputIfNone) {
        try{
            return httpConn.getResponseMessage();
        }catch(IOException e) {
            LOG.log(Level.WARNING, "Failed to retrieve response message", e);        
            return outputIfNone;
        }
    }

    @Override
    public URLConnection getUrlConnection() {
        return urlConnection;
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public int getCode() {
        return this.responseCode;
    }

    @Override
    public String getMessage() {
        return this.responseMessage;
    }

    @Override
    public List<String> getCookies() {
        return cookies;
    }
}
