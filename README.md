### imports

```java

package com.bc.net;

import com.bc.net.impl.RequestBuilderImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

```