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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 12:57:02 PM
 */
public interface RequestBodyWriter<T> {
    
    T back();

    RequestBodyWriter<T> boundary(String boundary);

    RequestBodyWriter<T> charset(String charset);

    default RequestBodyWriter<T> files(Map params) {
        params.forEach((k, v) -> this.file(k.toString(), (File)v));
        return this;
    }

    RequestBodyWriter<T> file(String name, File file);

    RequestBodyWriter<T> formDataSeparator(String separator);

    default RequestBodyWriter<T> multiPartParams(Map params) {
        params.forEach((k, v) -> this.multiPartParam(k.toString(), v.toString()));
        return this;
    }

    RequestBodyWriter<T> multiPartParam(String name, String value);

    default RequestBodyWriter<T> params(Map params, boolean encode) {
        params.forEach((k, v) -> this.param(k.toString(), v.toString(), encode));
        return this;
    }
    
    RequestBodyWriter<T> param(String name, String value, boolean encode);

    RequestBodyWriter<T> reset();

    default RequestBodyWriter<T> urls(Map params) {
        params.forEach((k, v) -> this.url(k.toString(), (URL)v));
        return this;
    }

    RequestBodyWriter<T> url(String name, URL url);

    default boolean hasOutput() {
        return this.hasMultiPartParameters() || 
                this.hasMultiPartResources() || 
                this.hasRequestParameters();
    }
    
    boolean hasMultiPartParameters();
    
    boolean hasMultiPartResources();
    
    boolean hasRequestParameters();
            
    default void write(URLConnection conn) throws UnsupportedEncodingException, IOException {
        if(this.hasOutput()) {
            try(OutputStream output = conn.getOutputStream()) {
                this.write(output);
            }        
        }
    }
    
    void write(OutputStream out) throws UnsupportedEncodingException, IOException;

    String getCharset();

    String getFormDataSeparator();

    String getBoundary();
}
