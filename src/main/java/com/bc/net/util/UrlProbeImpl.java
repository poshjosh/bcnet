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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 9, 2018 12:32:06 PM
 * @todo Disconnect and close where appropriate. 
 * @todo Consider setting follow redirect to 'true'
 */
public class UrlProbeImpl implements Serializable, UrlProbe {
    
    public UrlProbeImpl() { }

    @Override
    public boolean exists(URL url) throws IOException {
        boolean oldVal = HttpURLConnection.getFollowRedirects();
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = getHead(url);
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }finally{
            HttpURLConnection.setFollowRedirects(oldVal);
        }
    }

    @Override
    public String getContentType(URL url) throws IOException {
        boolean oldVal = HttpURLConnection.getFollowRedirects();
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = getHead(url);
            con.connect();
            return con.getContentType();
        }finally{
            HttpURLConnection.setFollowRedirects(oldVal);
        }
    }

    @Override
    public HttpURLConnection getHead(URL url) throws IOException {
        final HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setInstanceFollowRedirects(false);
        con.setRequestMethod("HEAD");
        return con;
    }

    @Override
    public boolean isValid(URL url) throws IOException {
        try{
            url.openConnection();
            return true;
        }catch(FileNotFoundException e) {
            return false;
        }
    }
}
