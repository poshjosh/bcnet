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
import java.util.concurrent.TimeUnit;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 6, 2018 1:55:13 PM
 */
public class Main {

    public static void main(String... args) throws IOException {
        
        final RequestBody formBody = new FormBody.Builder()
                .add("param_a", "value_a")
                .addEncoded("param_b", "value_b")
                .build();
//FormBody.create(MediaType.parse(""), ByteString.EMPTY);
        final File fileToUpload = new File("");
        
        final RequestBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("fieldName", fileToUpload.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), fileToUpload))
                    .build();

        final RequestBody jsonBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
            "{sampleText:\"text\"}");
        
        final String url = "http://www.looseboxes.com";
        
        final Request request = new Request.Builder()
            .addHeader("header_a", "value_a")  // to add header data
            .post(formBody)         // for form data
            .post(jsonBody)         // for json data
            .post(multipartBody)    // for multipart data
//            .url(new URL(null, url, new HttpStreamHandlerForBadStatusLine()))
            .url(url)
            .build();
        
        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        
        final OkHttpClient client = clientBuilder
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .cookieJar(CookieJar.NO_COOKIES)
                .build();

        final Response response = client.newCall(request).execute();
        
       
    }
}
