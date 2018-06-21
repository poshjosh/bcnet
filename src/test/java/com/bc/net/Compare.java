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

import com.bc.io.CharFileIO;
import com.bc.net.impl.RequestBuilderImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 10:38:43 PM
 */
public class Compare {

    final RequestBuilder reqBuilder = new RequestBuilderImpl();
    final ConnectionManager connMgr = new ConnectionManager();

    public static void main(String... args) {
        try{
            
            final Compare com = new Compare();
            
            final URL url_0 = new URL("http://www.looseboxes.com/idisc/feeds.jsp");

            com.x(url_0);
            
            final URL url_1 = new URL("http://www.looseboxes.com/idisc/login.jsp");

            com.x(url_1);
            
        }catch(IOException e) {
            
            e.printStackTrace();
        }
    }
    
    public void x(URL url) throws IOException {
        
        final CharFileIO io = new CharFileIO();

        try(InputStream in_0 = reqBuilder.getInputStream(url)) {

            final CharSequence cs_0 = io.readChars(in_0);
        }

        connMgr.setAddCookiesToRequest(true);
        connMgr.setLoadCookiesFromResponse(true);
        
        try(InputStream in_1 = connMgr.getInputStream(url)) {

            final CharSequence cs_1 = io.readChars(in_1);
        }
    }
}
