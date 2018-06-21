package com.bc.net;

import com.bc.htmlparser.ParseJob;
import com.bc.io.CharFileIO;
import com.bc.net.impl.RequestBuilderImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;


/**
 * @(#)MaintainSessionState.java   20-Jan-2015 09:02:18
 *
 * Copyright 2011 NUROX Ltd, Inc. All rights reserved.
 * NUROX Ltd PROPRIETARY/CONFIDENTIAL. Use is subject to license 
 * terms found at http://www.looseboxes.com/legal/licenses/software.html
 */

/**
 * @author   chinomso bassey ikwuagwu
 * @version  2.0
 * @since    2.0
 */
public class MaintainSessionStateExample {

    public static void main(String [] args) {
        
        new MaintainSessionStateExample().run();
    }

    public void run() {

        try{
            
            final String charset = "UTF-8";
            final String email = "posh.bc@gmail.com";
            final String pass = "1kjvdul-";
            URL url = new URL("http://www.looseboxes.com/idisc/login");
// Using this does not require the Map below            
//            URL url = new URL("http://www.looseboxes.com/idisc/login?emailaddress="+email+"&password="+pass);

            Map<String, String> output = new HashMap<>(4, 1.0f);
            output.put("emailaddress", email);
            output.put("emailAddress", email);
            output.put("password", pass);
            
            InputStream in;

            final RequestBuilder connSess = new RequestBuilderImpl();

            Response res = connSess
                    .formContentType(charset)
                    .url(url)
                    .body()
                    .params(output, false)
                    .back().response();

            print(connSess, res);
            
            in = res.getInputStream();
            
            System.out.println("\nAttempting to post new feed");

            url = new URL("http://www.looseboxes.com/idisc/newfeed.jsp");

            res = connSess
                    .formContentType(charset)
                    .url(url)
                    .response();

            print(connSess, res);
            
            in = res.getInputStream();
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void print(RequestBuilder connSess, Response res) throws IOException {
        
            final int code = res.getCode();
            final String msg = res.getMessage();
            final List<String> cookies = connSess.getCookies();
            
System.out.println("Response. Code: " + code + ", message: " + msg);
System.out.println("Cookies: "+cookies);
 
            CharFileIO io = new CharFileIO();
            
            String contents = io.readChars(res.getInputStream()).toString();
            
            final String [] titleAndMessage = getPageTitleAndMessage(contents);
System.out.println("Title: "+titleAndMessage[0]);
System.out.println("Contents: "+titleAndMessage[1]);
    }
    
    private String [] getPageTitleAndMessage(String input) {
        
        String title;
        
        ParseJob parseJob = new ParseJob();
        
        try{
            title = parseJob.plainText(true).accept(HTML.Tag.TITLE).separator("\n").parse(input).toString();
        }catch(IOException e) {
            title = "";
        }
        

        SimpleAttributeSet as = new SimpleAttributeSet();
        as.addAttribute("class", "content");
        
        String myMessage;
        try{
            myMessage = parseJob.plainText(true).accept(as).separator("\n").parse(input).toString();
        }catch(IOException e) {
            myMessage = "";
        }

        return new String[]{title, myMessage};
    }
}
