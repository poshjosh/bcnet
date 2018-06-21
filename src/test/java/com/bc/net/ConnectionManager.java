package com.bc.net;

import com.bc.net.impl.CookieProcessorImpl;
import com.bc.net.impl.RequestBodyWriterImpl;
import com.bc.net.util.UserAgents;
import com.bc.util.QueryParametersConverter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @(#)ConnectionManager.java   04-Apr-2013 20:05:27
 *
 * Copyright 2011 NUROX Ltd, Inc. All rights reserved.
 * NUROX Ltd PROPRIETARY/CONFIDENTIAL. Use is subject to license 
 * terms found at http://www.looseboxes.com/legal/licenses/software.html
 */
/**
 * Got some tips from
 * http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
 *@todo integrate CookieHandler with ConnectionManager
 *        CookieHandler cookieHandler = CookieHandler.getDefault();
 *        if(cookieHandler == null) {
 *            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
 *        }
 * @author   chinomso bassey ikwuagwu
 * @version  2.0
 * @since    2.0
 */
public class ConnectionManager {

    private static final Logger LOG = Logger.getLogger(ConnectionManager.class.getName());
    
    private boolean running;
    private boolean stopped;
    
    /**
     * If <tt>true</tt> the the cookies returned from the current connection
     * operation will be added to subsequent connections
     */
    private boolean addCookiesToRequest;
    
    /**
     * If <tt>true</tt> the the cookies will be extracted from the current
     * connection operation and may be accessed via the {@link #getCookies()}
     * method
     */
    private boolean loadCookiesFromResponse;
    
    private boolean mobile;
    
    private boolean generateRandomUserAgent;
    
    private UserAgents userAgents;
    
    private int connectTimeout = -1;
    private int readTimeout = -1;
    
    private int responseCode;
    
    private String responseMessage;
    
    private int chunkedStreamingBuffer;
    
    private int fixedLengthStreamingBuffer;
    
    private Set<String> cookies;
    
    private URLConnection connection;
    
    private InputStream inputStream;
    
    private RetryConnectionFilter retryAfterExceptionFilter;
    
    private final CookieProcessor cookieProcessor;
    
    public ConnectionManager() {
        this(0, 0);
    }
    
    public ConnectionManager(int maxRetrials, long retrialInterval) {
// Setting this caused the server to return code 400:Bad request        
//        this.chunkedStreamingBuffer = 8192;
        if(maxRetrials > 0) {
            this.retryAfterExceptionFilter = new RetryConnectionFilter(
                    maxRetrials, retrialInterval);
        }
        this.generateRandomUserAgent = true;
        this.userAgents = new UserAgents();
        this.cookies = new LinkedHashSet<>();
        this.cookieProcessor = new CookieProcessorImpl();
    }
    
    public void reset() {
        this.setRunning(false);
        this.setStopped(false);
        this.responseCode = 0;
        this.responseMessage = null;
        this.connection = null;
        this.inputStream = null;
// Cookies are available for reuse across connection operations
//        this.cookies.clear();
    }

    public static boolean exists(String URLName) throws IOException {
        boolean oldVal = HttpURLConnection.getFollowRedirects();
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = getHead(URLName);
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }finally{
            HttpURLConnection.setFollowRedirects(oldVal);
        }
    }

    public static String getContentType(String URLName) throws IOException {
        boolean oldVal = HttpURLConnection.getFollowRedirects();
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = getHead(URLName);
            con.connect();
            return con.getContentType();
        }finally{
            HttpURLConnection.setFollowRedirects(oldVal);
        }
    }

    private static HttpURLConnection getHead(String URLName) throws IOException {
        HttpURLConnection con = 
           (HttpURLConnection) new URL(URLName).openConnection();
        con.setInstanceFollowRedirects(false);
        con.setRequestMethod("HEAD");
        return con;
    }
    
    public static boolean isValidUrl(URL url) throws IOException {
        try{
            url.openConnection();
            return true;
        }catch(FileNotFoundException e) {
            return false;
        }
    }

    public void stop(long timeout) {
log(Level.FINE, "@stop(int). Is running: {0}", this.isRunning());
        if(!this.isRunning()) return; 

        this.setStopped(true);

log(Level.FINER, "@stop(int). Is running: {0}", this.isStopped());
    }
    
    public void flushAndClose(OutputStream out) {
        this.flush(out);
        this.close(out);
    }

    public void flush(OutputStream c) {
        if (c != null) {
            try {
                c.flush();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Exception flushing.", e);
            }
        }
    }

    public void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Exception closing.", e);
            }
        }
    }

    public void disconnect() {
        if(this.getConnection() instanceof HttpURLConnection) {
            ((HttpURLConnection)this.getConnection()).disconnect();
        }
    }

    /**
     * Sends the request with default request properties:<br/>
     * <tt>Accept-Charset</tt> = <tt>UTF-8</tt><br/>
     * <tt>Content-Type</tt> = <tt>application/x-www-form-urlencoded;charset=UTF-8</tt>
     * @throws IOException 
     */
    public InputStream getInputStreamForUrlEncodedForm(
            URL url, Map<String, String> outputParameters, boolean encode) 
            throws IOException {
    
        return this.getInputStreamForUrlEncodedForm(url, outputParameters, "UTF-8", encode);
    }

    /**
     * Sends the request with default request properties:<br/>
     * <tt>Accept-Charset</tt> = <tt>UTF-8</tt><br/>
     * <tt>Content-Type</tt> = <tt>multipart/form-data</tt>
     * @throws IOException 
     */
    public InputStream getInputStreamForMultipartForm(
            URL url, Map<String, String> outputParameters, 
            Map<String, File> outputFiles) throws IOException {
    
        String boundary = Long.toHexString(System.currentTimeMillis());
        
        return this.getInputStreamForMultipartForm(url, outputParameters, outputFiles, null, "UTF-8", boundary, true);
    }
    
    /**
     * Sends the request with default request properties:<br/>
     * <tt>Accept-Charset</tt> = <tt>UTF-8</tt><br/>
     * <tt>Content-Type</tt> = <tt>multipart/form-data</tt>
     * @throws IOException 
     */
    public InputStream getInputStreamForMultipartForm(
            URL url, Map<String, String> outputParameters, 
            Map<String, File> outputFiles, Map<String, URL> outputUrls) 
            throws IOException {
    
        String boundary = Long.toHexString(System.currentTimeMillis());
        
        return this.getInputStreamForMultipartForm(url, outputParameters, outputFiles, outputUrls, "UTF-8", boundary, true);
    }
    
    /**
     * Sends the request with default request properties:<br/>
     * <tt>Accept-Charset</tt> = <tt>[charset]</tt><br/>
     * <tt>Content-Type</tt> = <tt>application/x-www-form-urlencoded;charset=[charser]</tt>
     * @throws IOException 
     */
    public InputStream getInputStreamForUrlEncodedForm(
            URL url, Map<String, String> outputParameters, String charset, boolean encode) 
            throws IOException {
    
        return this.getInputStream(url, charset, "Content-Type", 
                "application/x-www-form-urlencoded;charset="+charset, 
                outputParameters, encode);
    }

    /**
     * Sends the request with default request properties:<br/>
     * <tt>Accept-Charset</tt> = <tt>[charset]</tt><br/>
     * <tt>Content-Type</tt> = <tt>multipart/form-data</tt>
     * @throws IOException 
     */
    public InputStream getInputStreamForMultipartForm(
            URL url, Map<String, String> outputParameters, 
            Map<String, File> outputFiles, Map<String, URL> outputUrls,
            String charset, String boundary, boolean encode) throws IOException {
    
        return this.getInputStream(url, charset, "Content-Type", 
                "multipart/form-data; boundary="+boundary,
                outputParameters, outputFiles, outputUrls, boundary, encode);
    }
    /*
     * Reads from (input) a URL
     */
    public InputStream getInputStream(
            URL url, String charset) throws IOException {

        return this.getInputStream(url, charset, null, null, null, false);
    }
    
    /*
     * Reads from (input) a URL
     */
    public InputStream getInputStream(
            URL url, String charset, String requestPropertyKey, Object requestPropertyValue) throws IOException {

        return this.getInputStream(url, charset, requestPropertyKey, requestPropertyValue, null, false);
    }

    /*
     * Reads from (input) a URL
     */
    public InputStream getInputStream(
            URL url, String charset, String requestPropertyKey, Object requestPropertyValue,
            Map<String, String> outputParameters, boolean encode) throws IOException {

        Map<String, Object> requestProperties = this.getRequestProperties(url, charset, requestPropertyKey, requestPropertyValue);
        
        return this.getInputStream(url, requestProperties, outputParameters, encode);
    }

    /*
     * Reads from (input) a URL
     */
    public InputStream getInputStream(
            URL url, String charset, String requestPropertyKey, Object requestPropertyValue,
            Map<String, String> outputParameters, Map<String, File> outputFiles,
            Map<String, URL> outputUrls, String boundary, boolean encode) throws IOException {

        Map<String, Object> requestProperties = this.getRequestProperties(url, charset, requestPropertyKey, requestPropertyValue);
        
        return this.getInputStream(url, requestProperties, outputParameters, outputFiles, outputUrls, boundary, encode);
    }
    
    public InputStream getInputStream(URL url) throws IOException {
        
        return this.getInputStream(url, null, null, false);
    }
    
    public InputStream getInputStream(
            URL url, Map<String, Object> requestProperties) 
            throws IOException {
        
        return this.getInputStream(url, requestProperties, null, false);
    }
    
    /**
     * Does writes to (output) and then reads from (input) a URL.
     * Sends a url-encoded request, using the POST method to the URL
     * Then returns an input stream for the feedback
     * @param url
     * @param requestProperties The Connection Properties e.g User-Agent, Content-Type etc
     * @param outputParameters The Request parameters you want to send
     * @param encode If true, the request parameter values will be encoded
     * @return An input stream to read from the response
     * @throws IOException 
     */
    public InputStream getInputStream(
            URL url, Map<String, Object> requestProperties, 
            Map<String, String> outputParameters, boolean encode) throws IOException {

        final URLConnection conn = this.openConnection(url, requestProperties, outputParameters, encode);
        
        return this.getInputStream(conn);
    }

    public URLConnection openConnection(
            URL url, Map<String, Object> requestProperties, 
            Map<String, String> outputParameters, boolean encode) throws IOException {

        URLConnection conn = this.openConnection(url, true, true, requestProperties);
        
this.log(Level.FINER, "Opened connection: {0}", conn);
        
        if(outputParameters != null && !outputParameters.isEmpty()) {
            
            ((HttpURLConnection)conn).setRequestMethod("POST");

            String charset = requestProperties == null || requestProperties.get("Accept-Charset") == null
                    ? "UTF-8" : requestProperties.get("Accept-Charset").toString();

this.log(Level.FINER, "Charset: {0}", charset);
           
            this.addParameters(conn, outputParameters, encode, charset);
        }
        
        return conn;
    }

    /**
     * Does writes to (output) and then reads from (input) a URL.
     * Sends a url-encoded request, using the POST method to the URL
     * Then returns an input stream for the feedback
     * @param url
     * @param requestProperties The Connection Properties e.g User-Agent, Content-Type etc
     * @param outputParameters The Request parameters you want to send
     * @param outputFiles The Files you want to send
     * @param boundary
     * @param encode If true, the request parameter values will be encoded
     * @return An input stream to read from the response
     * @throws IOException 
     */
    public InputStream getInputStream(
            URL url, Map<String, Object> requestProperties, 
            Map<String, String> outputParameters, 
            Map<String, File> outputFiles,
            String boundary,
            boolean encode) throws IOException {

        return this.getInputStream(url, requestProperties, outputParameters, outputFiles, null, boundary, encode);
    }
    
    /**
     * Does writes to (output) and then reads from (input) a URL.
     * Sends a url-encoded request, using the POST method to the URL
     * Then returns an input stream for the feedback
     * @param url
     * @param requestProperties The Connection Properties e.g User-Agent, Content-Type etc
     * @param outputParameters The Request parameters you want to send
     * @param outputFiles The Files you want to send
     * @param outputUrls Links to the remote files you want to send
     * @param boundary The boundary to use for the multipart output
     * @param encode If true, the request parameter values will be encoded
     * @return An input stream to read from the response
     * @throws IOException 
     */
    public InputStream getInputStream(
            URL url, Map<String, Object> requestProperties, 
            Map<String, String> outputParameters, 
            Map<String, File> outputFiles,
            Map<String, URL> outputUrls, 
            String boundary,
            boolean encode) throws IOException {
        
        URLConnection conn = this.openConnection(url, requestProperties, outputParameters, outputFiles, outputUrls, boundary, encode);
        
        return this.getInputStream(conn);
    }
    
    public URLConnection openConnection(
            URL url, Map<String, Object> requestProperties, 
            Map<String, String> outputParameters, 
            Map<String, File> outputFiles,
            Map<String, URL> outputUrls, 
            String boundary,
            boolean encode) throws IOException {
        
        URLConnection conn = this.openConnection(url, true, true, requestProperties);
        
this.log(Level.FINER, "Opened connection: {0}", conn);
        
        if((outputParameters != null && !outputParameters.isEmpty()) ||
                (outputFiles != null && !outputFiles.isEmpty()) || 
                (outputUrls != null && !outputUrls.isEmpty())) {
            
            ((HttpURLConnection)conn).setRequestMethod("POST");

            String charset = requestProperties == null || requestProperties.get("Accept-Charset") == null
                    ? "UTF-8" : requestProperties.get("Accept-Charset").toString();

this.log(Level.FINER, "Charset: {0}", charset);

            this.addMultipartParameters(conn, outputParameters, outputFiles, outputUrls, charset, boundary);
        }
        
        return conn;
    }
    
    public URLConnection openConnection(URL url, boolean doOutput, boolean doInput, 
            String charset, String key, Object value) throws IOException {

        Map<String, Object> requestProperties = (Map<String, Object>)this.getRequestProperties(url, charset, key, value);
        
        return this.openConnection(url, doOutput, doInput, requestProperties);
    }

    public URLConnection openConnection(
            URL url, boolean doOutput, boolean doInput, 
            Map<String, Object> requestProperties) throws IOException {
        
        this.setRunning(true);

        try{

            return this.doOpenConnection(url, doOutput, doInput, requestProperties);

//        }catch(IOException e) {
//            if(this.retryAfterExceptionFilter != null && this.retryAfterExceptionFilter.accept(e)) {
//                log(Level.WARNING, "{0}, Retrying URL: {1}", e, url);
//                return this.openConnection(url, doOutput, doInput, requestProperties);
//            }else {
//                throw e;
//            }
        }finally{
            this.setRunning(false);
        }
    }
    
    public OutputStream getOutputStream(URL url, boolean doOutput, boolean doInput,
            String charset, String key, Object value) throws IOException {

        Map<String, Object> requestProperties = (Map<String, Object>)this.getRequestProperties(url, charset, key, value);
        
        return this.getOutputStream(url, doOutput, doInput, requestProperties);
    }

    public OutputStream getOutputStream(URL url, boolean doOutput, boolean doInput,
            Map<String, Object> requestProperties) throws IOException {
        
        this.setRunning(true);

        try{

            URLConnection conn = this.openConnection(url, doOutput, doInput, requestProperties);

            return this.getOutputStream(conn);

        }catch(IOException e) {
            if(this.retryAfterExceptionFilter != null && this.retryAfterExceptionFilter.accept(e)) {
                log(Level.WARNING, "{0}, Retrying URL: {1}", e, url);
                return this.getOutputStream(url, doOutput, doInput, requestProperties);
            }else {
                throw e;
            }
        }finally{
            this.setRunning(false);
        }
    }
    
    public OutputStream getOutputStream(URLConnection connection) throws IOException {

        this.setRunning(true);

        try{

            return connection.getOutputStream();

        }catch(IOException e) {
            if(this.retryAfterExceptionFilter != null && this.retryAfterExceptionFilter.accept(e)) {
                log(Level.WARNING, "{0}, Retrying URL: {1}", e, connection.getURL());
                return this.getOutputStream(connection);
            }else {
                throw e;
            }
        }finally{
            this.setRunning(false);
        }
    }

    public InputStream getInputStream() throws IOException {
        if(this.inputStream == null) {
            URLConnection conn = this.getConnection();
            if(conn != null) {
                this.inputStream = this.getInputStream(conn);
            }
        }
        return this.inputStream;
    }
    
    public InputStream getInputStream(URLConnection connection) throws IOException {

final long mb4 = this.availableMemory();        
final long tb4 = System.currentTimeMillis();  
        
        this.setRunning(true);

        try{
            
            final com.bc.net.Response res = new com.bc.net.impl.ResponseImpl(connection, this.cookieProcessor);
            
            if(this.loadCookiesFromResponse) {
                this.cookies.addAll(res.getCookies());
                LOG.info(() -> "Loaded cookies from response: " + res.getCookies());
            }
            
            this.responseCode = res.getCode();
            this.responseMessage = res.getMessage();
            
            this.inputStream = res.getInputStream();
            
log(Level.FINER, 
"Done getting input stream. Spent, time: {0}, memory: {1}", 
System.currentTimeMillis()-tb4, this.usedMemory(mb4));
            
            return this.inputStream;
            
        }catch(IOException e) {
            
            if(this.retryAfterExceptionFilter != null && this.retryAfterExceptionFilter.accept(e)) {
                
log(Level.WARNING, "{0}, Retrying URL: {1}", e, connection.getURL());
                
                return this.getInputStream(connection);
                
            }else {
                
                throw e;
            }
        }finally{
            
            this.setRunning(false);
        }
    }
    
    protected URLConnection doOpenConnection(URL url, boolean doOutput, boolean doInput, 
            Map<String, Object> requestProperties) throws IOException {
        
        requestProperties = requestProperties == null ? null : new HashMap(requestProperties);

final long mb4 = this.availableMemory();         
final long tb4 = System.currentTimeMillis();  
        this.connection = url.openConnection(); 
log(Level.FINER, 
"Opened connection. Spent, time: {0}, memory: {1}, connection: {2}", 
System.currentTimeMillis()-tb4, this.usedMemory(mb4), connection);

        connection.setDoOutput(doOutput); 

        connection.setDoInput(doInput);
        
log(Level.FINER, "Streaming FixedLength: {0}, Chunked: {1}", 
        this.getFixedLengthStreamingBuffer(), this.getChunkedStreamingBuffer());

        if(connection instanceof HttpURLConnection) {
            HttpURLConnection httpConn =((HttpURLConnection)connection);
            if(this.getFixedLengthStreamingBuffer() > 0) {
                (httpConn).setFixedLengthStreamingMode(this.getFixedLengthStreamingBuffer());
            }else if(this.getChunkedStreamingBuffer() > 0){
                (httpConn).setChunkedStreamingMode(this.getChunkedStreamingBuffer());
            }
            
            if(readTimeout > -1) {
                httpConn.setReadTimeout(readTimeout);
            }
            if(connectTimeout > -1) {
                httpConn.setConnectTimeout(connectTimeout);
            }
        }

        if(this.generateRandomUserAgent) {
            if(requestProperties == null) {
                requestProperties = new HashMap<>();
            }
            this.addRandomUserAgent(connection.getURL(), requestProperties);
        }
        this.populateConnection(connection, requestProperties);

        if(this.addCookiesToRequest) {
            LOG.info(() -> "Adding cookies to request: " + cookies);
            this.cookieProcessor.addCookiesToRequest(connection, cookies);
        }
        
        return connection;
    }
    
    private Map<String, Object> getRequestProperties(URL url, String charset, String key, Object value) {
        Map requestProperties = new HashMap();
        if(charset != null) {
            requestProperties.put("Accept-Charset", charset);
        }
        if(value != null) {
            requestProperties.put(key, value);
        }
        if(this.generateRandomUserAgent) {
            this.addRandomUserAgent(url, requestProperties);
        }
        return requestProperties;
    }
    
    private void populateConnection(URLConnection connection, Map<String, Object> requestProperties) {
final long mb4 = this.availableMemory();         
final long tb4 = System.currentTimeMillis();  
        if(requestProperties == null) {
            return;
        }
        for(String key:requestProperties.keySet()) {
            Object val = requestProperties.get(key);
            if(val != null) {
log(Level.FINER, "Settting request property: [{0}={1}]", key, val);                
                connection.setRequestProperty(key, val.toString());
            }
        }
log(Level.FINER, 
"Populated connection with request properties. Spent, time: {0}, memory: {1}", 
System.currentTimeMillis()-tb4, this.usedMemory(mb4));
    }
    
    private Object addRandomUserAgent(URL url, Map<String, Object> requestProperties) {
        if(this.generateRandomUserAgent) {
            Object userAgent = requestProperties.get("User-Agent");
            if(userAgent == null) {
                userAgent = requestProperties.get("user-agent");
                if(userAgent == null) {
                    userAgent = url == null ? this.userAgents.getAny(mobile) :
                            this.userAgents.getAny(url, mobile);
                    return requestProperties.put("User-Agent", userAgent);
                }
            }else{
                return userAgent;
            }
        }
        return null;
    }

    /**
     * Writes url-encoded content to a URLConnection's output stream
     * @param conn The URLConnection whose outputstream will be written to
     * @param reqParams The Request parameters you want to send
     * @param encode If true, the request parameter values will be encoded
     * @param charset The charset to use for encoding the content
     * @throws UnsupportedEncodingException
     * @throws IOException 
     * @throws NullPointerException If charset is null
     */
    public void addParameters(
            URLConnection conn, Map<String, String> reqParams, 
            boolean encode, String charset) 
            throws UnsupportedEncodingException, IOException {
        OutputStream output = null;
        try{
            output = this.getOutputStream(conn);
            this.addParameters(output, reqParams, encode, charset);
        }finally{
            this.flushAndClose(output);
        }
    }

    /**
     * Writes url-encoded content to a URLConnection's output stream
     * @param output The outputstream to be written to
     * @param reqParams The Request parameters you want to send
     * @param encode If true, the request parameter values will be encoded
     * @param charset The charset to use for encoding the content
     * @throws UnsupportedEncodingException
     * @throws IOException 
     * @throws NullPointerException If charset is null
     */
    public void addParameters(
            OutputStream output, Map<String, String> reqParams, 
            boolean encode, String charset) 
            throws UnsupportedEncodingException, IOException {
        
final long mb4 = this.availableMemory();         
final long tb4 = System.currentTimeMillis();  

        if(reqParams == null || reqParams.isEmpty()) {
            return;
        }
        
        if(charset == null) {
            throw new NullPointerException();
        }
        
        String query = this.getQueryString(reqParams, encode, charset);
        
        output.write(query.getBytes(charset));
log(Level.FINER, 
"Written query to output stream. Spent, time: {0}, memory: {1}. Query: {2}", 
System.currentTimeMillis()-tb4, this.usedMemory(mb4), query);
    }

    public void addMultipartURLs(
            URLConnection conn, Map<String, String> nonFiles, Map<String, URL> urls, 
            String charset, final String boundary) throws IOException {
        OutputStream output = null;
        try{
            output = this.getOutputStream(conn);
            this.addMultipartURLs(output, nonFiles, urls, charset, boundary);
        }finally{
            this.flushAndClose(output);
        }
    }
    
    public void addMultipartURLs(
            OutputStream output, Map<String, String> nonFiles, Map<String, URL> urls, 
            String charset, final String boundary) throws IOException {
        
        this.addMultipartParameters(output, nonFiles, null, urls, charset, boundary);
    }

    public void addMultipartFiles(
            URLConnection conn, Map<String, String> nonFiles, Map<String, File> files, 
            String charset, final String boundary) throws IOException {
        OutputStream output = null;
        try{
            output = this.getOutputStream(conn);
            this.addMultipartFiles(output, nonFiles, files, charset, boundary);
        }finally{
            this.flushAndClose(output);
        }
    }
    
    public void addMultipartFiles(
            OutputStream output, Map<String, String> nonFiles, Map<String, File> files, 
            String charset, final String boundary) throws IOException {
        
        this.addMultipartParameters(output, nonFiles, files, null, charset, boundary);
    }
    
    public void addMultipartParameters(
            URLConnection conn, Map<String, String> nonFiles, 
            Map<String, File> files, Map<String, URL> urls, 
            String charset, final String boundary) throws IOException {
        OutputStream output = null;
        try{
            output = this.getOutputStream(conn);
            this.addMultipartParameters(output, nonFiles, files, urls, charset, boundary);
        }finally{
            this.flushAndClose(output);
        }
    }
    
    public void addMultipartParameters(
            OutputStream output, Map<String, String> nonFiles, 
            Map<String, File> files, Map<String, URL> urls, 
            String charset, final String boundary) throws IOException {

        new RequestBodyWriterImpl()
                .multiPartParams(nonFiles)
                .files(files)
                .urls(urls)
                .charset(charset)
                .boundary(boundary)
                .write(output);
    }
    
    public String getQueryString(
            Map<String, String> reqParams, 
            final boolean encode, final String charset) 
            throws UnsupportedEncodingException, IOException {

final long mb4 = this.availableMemory();         
final long tb4 = System.currentTimeMillis();  
        
log(Level.FINER, "About to create query string for parameters, encode: {0}", encode);

        final QueryParametersConverter fmt = new QueryParametersConverter("&");
        
        final String query = fmt.convert(reqParams, encode, charset);

log(Level.FINER, 
"Created query string from request parameters. Spent, time: {0}, memory: {1}.\nParameters: {2}\nQuery string: {3}", 
System.currentTimeMillis()-tb4, this.usedMemory(mb4), reqParams, query);
        
        return query;
    }

    public URLConnection getConnection() {
        return connection;
    }
    
    public boolean isPositiveCompletion() {
        final int code = this.getResponseCode();
        return code > 0 && code < 300;
    }
    
    public int getResponseCode() {
        if(responseCode == 0) {
            try{
                this.getInputStream();
            }catch(IOException e){
                log(e);
            }
        }
        return responseCode;
    }
    
    public String getResponseMessage() {
        if(responseMessage == null) {
            try{
                this.getInputStream();
            }catch(IOException e){
                log(e);
            }
        }
        return responseMessage;
    }
    
    private transient static final Runtime runtime = Runtime.getRuntime();
    
    public final long usedMemory(long bookmarkMemory) {
        return bookmarkMemory - availableMemory();
    }
    
    public final long availableMemory() {
        final long totalMemory = runtime.totalMemory(); // current heap allocated to the VM process
        final long freeMemory = runtime.freeMemory(); // out of the current heap, how much is free
        final long maxMemory = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        final long usedMemory = totalMemory - freeMemory; // how much of the current heap the VM is using
        final long availableMemory = maxMemory - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
        return availableMemory;
    }  

    protected void log(Exception e) {
        log("", e);
    }
    
    protected void log(String msg, Exception e) {
        LOG.log(Level.WARNING, msg, e);
    }

    protected void log(Level level, String fmt, Object val_0) {
        LOG.log(level, fmt, val_0);
    }

    protected void log(Level level, String fmt, Object val_0, Object val_1) {
        LOG.log(level, () -> MessageFormat.format(fmt, val_0, val_1));
    }

    protected void log(Level level, String fmt, Object val_0, Object val_1, Object val_2) {
        LOG.log(level, () -> MessageFormat.format(fmt, val_0, val_1, val_2));
    }

    protected void log(Level level, String fmt, Object val_0, Object val_1, Object val_2, Object val_3) {
        LOG.log(level, () -> MessageFormat.format(fmt, val_0, val_1, val_2, val_3));
    }
    
    public int getMaxTrials() {
        return this.retryAfterExceptionFilter.getMaxTrials();
    }

    public long getSleepTime() {
        return this.retryAfterExceptionFilter.getSleepTime();
    }
    
    public List<String> getCookies() {
        return new ArrayList(cookies);
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return 
     * @see #loadCookiesFromResponse
     */
    public boolean isLoadCookiesFromResponse() {
        return loadCookiesFromResponse;
    }

    /**
     * @param loadCookiesFromResponse 
     * @see #loadCookiesFromResponse
     */
    public void setLoadCookiesFromResponse(boolean loadCookiesFromResponse) {
        this.loadCookiesFromResponse = loadCookiesFromResponse;
    }

    /**
     * @return 
     * @see #addCookiesToRequest
     */
    public boolean isAddCookiesToRequest() {
        return addCookiesToRequest;
    }

    /**
     * @param setCookies
     * @see #addCookiesToRequest
     */
    public void setAddCookiesToRequest(boolean setCookies) {
        this.addCookiesToRequest = setCookies;
    }

    public int getChunkedStreamingBuffer() {
        return chunkedStreamingBuffer;
    }

    public void setChunkedStreamingBuffer(int chunkedStreamingBuffer) {
        this.chunkedStreamingBuffer = chunkedStreamingBuffer;
    }

    public int getFixedLengthStreamingBuffer() {
        return fixedLengthStreamingBuffer;
    }

    public void setFixedLengthStreamingBuffer(int fixedLengthStreamingBuffer) {
        this.fixedLengthStreamingBuffer = fixedLengthStreamingBuffer;
    }

    public RetryConnectionFilter getRetryAfterExceptionFilter() {
        return retryAfterExceptionFilter;
    }

    public void setRetryAfterExceptionFilter(RetryConnectionFilter retryAfterExceptionFilter) {
        this.retryAfterExceptionFilter = retryAfterExceptionFilter;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isMobile() {
        return mobile;
    }

    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    public boolean isGenerateRandomUserAgent() {
        return generateRandomUserAgent;
    }

    public void setGenerateRandomUserAgent(boolean generateRandomUserAgent) {
        this.generateRandomUserAgent = generateRandomUserAgent;
    }

    public UserAgents getUserAgents() {
        return userAgents;
    }

    public void setUserAgents(UserAgents userAgents) {
        this.userAgents = userAgents;
    }
}
/**
 * 
    public void setMaxTrials(int i) {
        this.retryAfterExceptionFilter = new RetryConnectionFilter(
                this.retryAfterExceptionFilter.isRetryNoConnectionException(),
                this.retryAfterExceptionFilter.isRetryFailedConnectionException(),
                i,
                this.retryAfterExceptionFilter.getSleepTime()
        );
    }

    public void setSleepTime(long millis) {
        this.retryAfterExceptionFilter = new RetryConnectionFilter(
                this.retryAfterExceptionFilter.isRetryNoConnectionException(),
                this.retryAfterExceptionFilter.isRetryFailedConnectionException(),
                this.retryAfterExceptionFilter.getMaxTrials(),
                millis
        );
    }
    
 * 
 */