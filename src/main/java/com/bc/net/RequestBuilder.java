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

package com.bc.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 7:12:14 PM
 */
public interface RequestBuilder {

    RequestBuilder clearCookies();
        
    RequestBuilder cookieProcessor(CookieProcessor cookieProcessor);
        
    RequestBodyWriter<RequestBuilder> body();
    
    default InputStream getInputStream() throws IOException {
        return this.response().getInputStream();
    }
    
    default InputStream getInputStream(URL url) throws IOException {
        return this.response(url).getInputStream();
    }
    
    default Response response(URL url) throws IOException {
        return this.url(url).response(); 
    }
    
    Response response() throws IOException;
    
    URLConnection build() throws IOException;
    
    RequestBuilder reset();
    
    default RequestBuilder post() {
        return this.method("POST");
    }
    
    default RequestBuilder get() {
        return this.method("GET");
    }

    RequestBuilder method(String method);
    
    RequestBuilder followRedirects(boolean follow);
    
    default RequestBuilder multiPartContentType(String boundary) {
        return this.add("Content-Type", "multipart/form-data; boundary="+boundary);
    }
    
    default RequestBuilder formContentType(String charset) {
        Objects.requireNonNull(charset);
        this.charset(charset);
        return this.add("Content-Type", "application/x-www-form-urlencoded;charset="+charset);
    }    

    default RequestBuilder charset(String charset) {
        this.body().charset(charset);
        return this.add("Accept-Charset", charset);
    }

    default RequestBuilder userAgent(String userAgent) {
        return this.add("User-Agent", userAgent);
    }
    
    default RequestBuilder addAll(Map<String, Object> all) {
        all.forEach((k, v) -> { this.add(k, v); });
        return this;
    }

    RequestBuilder add(String key, Object value);
    
    RequestBuilder url(URL url);

    RequestBuilder chunkedStreamingBuffer(int chunkedStreamingBuffer);

    RequestBuilder connectTimeout(int connectTimeout);

    RequestBuilder addCookies(Collection<String> cookiesToAdd);

    RequestBuilder fixedLengthStreamingBuffer(int fixedLengthStreamingBuffer);

    RequestBuilder readTimeout(int readTimeout);
    
    RequestBuilder randomUserAgent(boolean random);
    
    RequestBuilder mobileUserAgent(boolean mobile);

    CookieProcessor getCookieProcessor();
    
    int getChunkedStreamingBuffer();

    int getConnectTimeout();

    List<String> getCookies();

    int getFixedLengthStreamingBuffer();

    String getMethod();

    int getReadTimeout();

    Map<String, Object> getRequestProperties();

    URL getUrl();

    boolean isGenerateRandomUserAgent();

    boolean isMobileUserAgent();
}
