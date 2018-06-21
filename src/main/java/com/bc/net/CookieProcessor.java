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

import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 12:08:01 PM
 */
public interface CookieProcessor 
        extends Function<URLConnection, List<String>>,
        BiConsumer<URLConnection, Collection<String>> {
    
    CookieProcessor NO_COOKIES = new CookieProcessor() {
        @Override
        public void addCookiesToRequest(URLConnection connection, Collection<String> cookies) { }
        @Override
        public List<String> loadCookiesFromResponse(URLConnection connection) { return Collections.EMPTY_LIST; }
    };

    @Override
    default List<String> apply(URLConnection connection) {
        return this.loadCookiesFromResponse(connection);
    }

    @Override
    default void accept(URLConnection connection, Collection<String> cookies) {
        this.addCookiesToRequest(connection, cookies);
    }

    void addCookiesToRequest(URLConnection connection, Collection<String> cookies);

    List<String> loadCookiesFromResponse(URLConnection connection);
}
