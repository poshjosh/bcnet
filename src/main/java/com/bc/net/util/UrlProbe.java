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

package com.bc.net.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 12:31:50 PM
 */
public interface UrlProbe {

    boolean exists(URL url) throws IOException;

    String getContentType(URL url) throws IOException;

    HttpURLConnection getHead(URL url) throws IOException;

    boolean isValid(URL url) throws IOException;
}
