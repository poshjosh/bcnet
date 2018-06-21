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

import com.bc.net.impl.RequestBuilderImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 12:24:13 PM
 */
public class ReadMe {

    public static void main(String... args) {
        
        try{
            
            final RequestBuilder req = new RequestBuilderImpl();

            final Response res = req
                    .connectTimeout(10_000)
                    .readTimeout(30_000)
                    .randomUserAgent(true)
                    .post()
                    .formContentType(StandardCharsets.UTF_8.name())
                    .add("requestPropertyName", "requestPropertyValue")
                    .body()
                    .multiPartParam("multipartParamName", "multipartParamValue")
                    .file("name", new File("filepath"))
                    .url("name", new URL("link"))
                    .back()
                    .response();
            
            final InputStream inputStream = res.getInputStream();
            
            final int responseCode = res.getCode();
            
            final String responseMessage = res.getMessage();
            
            final List<String> cookies = res.getCookies();
            
        }catch(IOException e) {
            
            e.printStackTrace();
        }
    }
}
