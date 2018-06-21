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
import java.io.Serializable;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 10:50:06 AM
 */
public class CookieProcessorImpl implements Serializable, CookieProcessor {

    private transient static final Logger LOG = Logger.getLogger(CookieProcessorImpl.class.getName());

    @Override
    public List<String> loadCookiesFromResponse(URLConnection connection) {

        final Map<String, List<String>> headerFields = connection.getHeaderFields(); 

        final List<String> cookieList = headerFields == null ? null : headerFields.get("Set-Cookie");
        
        return cookieList == null || cookieList.isEmpty() ? Collections.EMPTY_LIST : 
                cookieList.size() == 1 ? Collections.singletonList(cookieList.get(0)) :
                Collections.unmodifiableList(cookieList);
    }

    @Override
    public void addCookiesToRequest(URLConnection connection, Collection<String> cookies) {
        
        for (String cookie : cookies) {
            
            // The split(";", 2)[0] is there to get rid of cookie attributes which are
            // irrelevant for the server side like expires, path, etc.
            final String value = cookie.substring(0, cookie.indexOf(';'));
            
            LOG.finer(() -> "Adding cookie: " + value + ", from raw: " + cookie);

            connection.addRequestProperty("Cookie", value);
        }
    }
}
