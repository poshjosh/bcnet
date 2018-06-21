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

package com.bc.net.cloudflare;

import com.bc.net.impl.ResponseImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import com.bc.net.RequestBuilder;
import com.bc.net.impl.RequestBuilderImpl;
import java.nio.charset.StandardCharsets;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 10:41:33 AM
 */
public class CloudFlareResponse extends ResponseImpl {

    private transient static final Logger LOG = Logger.getLogger(CloudFlareResponse.class.getName());

    /**
     * CloudFlare expects a response after a delay
     */
    private final int delay;
    
    private final String charset;

    private final RequestBuilder requestBuilder;
    
    public CloudFlareResponse(URL url) throws IOException { 
        this(new RequestBuilderImpl(), url, 6000, StandardCharsets.UTF_8);
    }

    public CloudFlareResponse(RequestBuilder requestBuilder, 
            URL url, int delay, Charset charset) throws IOException { 
        this(requestBuilder, requestBuilder.url(url).build(), delay, charset);
    }

    public CloudFlareResponse(RequestBuilder requestBuilder, 
            URLConnection urlConn, int delay, Charset charset) 
        throws IOException {
        super(urlConn, requestBuilder.getCookieProcessor());
        this.delay = delay;
        this.requestBuilder = Objects.requireNonNull(requestBuilder);
        this.requestBuilder.charset(charset.name());
        this.charset = charset.name();
    }
    
    @Override
    public InputStream getInputStream(URLConnection originalConnection) throws IOException {
        
        final InputStream originalStream = super.getInputStream(originalConnection);
        
        final HttpURLConnection httpConn;
        if(originalConnection instanceof HttpURLConnection) {
            httpConn = (HttpURLConnection)originalConnection;
        }else{
            httpConn = null;
        }
        
        if(httpConn != null && httpConn.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
            
            return originalStream;
        }
        
        final String contents = this.readAll(originalStream);
        
        final URL url = originalConnection.getURL();

        final CloudFlareResponseParameters outputParameters = new CloudFlareResponseParameters();
        
        try{
            
            outputParameters.generate(url, contents);
            
        }catch(ScriptException e) {
            
            LOG.log(Level.WARNING, "Exception generating cloudflare response", e);
            
            return originalStream;
        }
        
        if(!outputParameters.isEmpty()) {

            if(delay > 0) {
                // 
                try{
                    Thread.sleep(delay);
                }catch(InterruptedException e) {
                    LOG.log(Level.WARNING, "Thread.sleep("+delay+") threw Exception", e);
                }
            }

            final URLConnection updatedConnection = requestBuilder
                    .add("Referer", url)
                    .randomUserAgent(true)
                    .url(url)
                    .body()
                    .params(outputParameters, true)
                    .back()
                    .build();
            
            return super.getInputStream(updatedConnection);
            
        }else{
            
            final String msg = "Recieved response 'Forbidden (403)' from remote server. However could not find cloud flare javascript challenge within response content";
            
            LOG.log(Level.FINE, msg+"\n{0}", contents);
            
            return originalStream;
        }
    }

    private String readAll(InputStream in) throws IOException {
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        final byte[] buffer = new byte[4096];
        
        int read = 0;
        
        while ((read = in.read(buffer, 0, buffer.length)) != -1) {
        
            baos.write(buffer, 0, read);
        }
        
        baos.flush();		
        
        return  new String(baos.toByteArray(), charset);
    }
}
