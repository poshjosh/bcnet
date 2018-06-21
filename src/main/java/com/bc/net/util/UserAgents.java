package com.bc.net.util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author USER
 */
public class UserAgents implements Serializable {
    
    private Map<String, String> cache;

    public UserAgents() { }

    /**
     * @param url
     * @param mobile
     * @return A randomly selected User-Agent String.
     * @throws java.net.MalformedURLException
     * @see #getAny(java.net.URL, boolean) 
     */
    public String getAny(String url, boolean mobile) throws MalformedURLException {

        return getAny(new URL(url), mobile);
    }
    
    /**
     * @param url
     * @param mobile
     * @return A randomly selected User-Agent String.
     */
    public String getAny(URL url, boolean mobile) {
        
        String baseUrl = this.getBaseUrl(url);
        
        String output = null;
        if(cache != null && baseUrl != null) {
            String cachedUa = cache.get(baseUrl);
            if(cachedUa != null) {
                output = cachedUa;
            }
        }
        if(output == null){
            output = this.getAny(mobile);
            if(baseUrl != null) {
                if(cache == null) {
                    cache = new HashMap<>();
                }
                cache.put(baseUrl, output);
            }
        }
        return output;
    }
    
    private String getBaseUrl(URL url) {
        try{
            return url.getProtocol() + "://"+url.getHost()==null?url.getAuthority():url.getHost();
        }catch(Exception ignored) {
            return null;
        }
    }
    
    /**
     * @param mobile
     * @return A randomly selected User-Agent String.
     */
    public String getAny(boolean mobile) {
        String [] userAgents = this.getAll(mobile);
        int index = (int)Math.round(Math.random() * userAgents.length);
        if(index >= userAgents.length) {
            index = userAgents.length -1;
        }
        return userAgents[index];
    }

    public String [] getAll(boolean mobile) {
        if(mobile) {
            return this.getAllMobile();
        }else{
            return this.getAllNonMobile();
        }
    }
    
    private String [] _ua;
    private String [] getAllNonMobile() {
        if(_ua == null) {
            _ua = new String[]{
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.A.B.C Safari/525.13",
                "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/525.19 (KHTML, like Gecko) Chrome/0.4.154.29 Safari/525.19",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.204 Safari/534.16",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.133 Safari/534.16",
                "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Ubuntu/10.10 Chromium/10.0.648.133 Chrome/10.0.648.133 Safari/534.16",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.151 Safari/534.16",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.134 Safari/534.16",
                "Mozilla/5.0 (X11; U; Linux i686 (x86_64); en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.634.0 Safari/534.16",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.45 Safari/534.16",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.04 (lucid) Firefox/3.6.13 GTB5",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8",
                "Mozilla/5.0 (Windows; U; Windows NT 6.0; fr; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 ( .NET CLR 3.5.30729; .NET4.0C)",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; de-DE; rv:1.9.2.9) Gecko/20100824 Firefox/3.6.9 (.NET CLR 3.5.30729)",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.13) Gecko/20101203 BTRS32096 Firefox/3.6.13",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.12) Gecko/20101027 Ubuntu/8.04 (hardy) Firefox/3.6.12",
                "Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 ( .NET CLR 3.5.30729; .NET4.0C)",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.12) Gecko/20101026 Firefox/3.6.12 ( .NET CLR 3.5.30729; .NET4.0E)",
                "Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.4; fr; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.2.15) Gecko/20110303 Firefox/3.6.15"
            };
        }
        return _ua;
    }

    private String [] _mua;
    private String [] getAllMobile() {
        if(_mua == null) {
            _mua = new String[]{
                "BlackBerry9000/4.6.0.65 Profile/MIDP-2.0 Configuration/CLDC-1.1 VendorID/102",
                "BlackBerry9700/5.0.0.442 Profile /MIDP-2.1 Configuration/CLDC-1.1 VendorID/-1",
            };
        }
        return _mua;
    }
}
