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

package com.bc.net.impl;

import com.bc.net.util.QueryParametersConverter;
import com.bc.net.RequestBodyWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Chinomso Bassey Ikwuagwu on Jun 7, 2018 12:31:41 PM
 */
public class RequestBodyWriterImpl<T> implements RequestBodyWriter<T> {

    private transient static final Logger LOG = Logger.getLogger(RequestBodyWriterImpl.class.getName());
    
    public static interface Resource {
        String getParamName();
        String getResourceName();
        InputStream getInputStream() throws IOException;
        default String getContentType(String outputIfUnknown) {
            String output;
            try{
                output = URLConnection.guessContentTypeFromStream(this.getInputStream());
            }catch(IOException e) {
                output = URLConnection.guessContentTypeFromName(this.getResourceName());
            }
            return output == null ? outputIfUnknown : output;
        }
    }
    
    private static class FileResource implements Resource {
        private final String paramName;
        private final File file;
        private FileResource(String paramName, File file) {
            this.paramName = Objects.requireNonNull(paramName);
            this.file = Objects.requireNonNull(file);
        }
        @Override
        public String getParamName() { return this.paramName; }
        @Override
        public String getResourceName() { return file.getName(); }
        @Override
        public InputStream getInputStream() throws FileNotFoundException { return new FileInputStream(file); }
    }
    
    private static class UrlResource implements Resource {
        private final String paramName;
        private final URL url;
        private UrlResource(String paramName, URL url) {
            this.paramName = Objects.requireNonNull(paramName);
            this.url = Objects.requireNonNull(url);
        }
        @Override
        public String getParamName() { return this.paramName; }
        @Override
        public String getResourceName() { return url.getPath(); }
        @Override
        public InputStream getInputStream() throws IOException { return url.openStream(); }
    }

    private final StringBuilder paramBuffer = new StringBuilder();
    
    private final StringBuilder multiPartParamBuffer = new StringBuilder();
    
    private final List<Resource> resources = new ArrayList<>();
    
    private final QueryParametersConverter queryBuilder;
    
    private final T back;
    
    private String charset;
    
    /**
     * Line separator required by multipart/form-data.
     */
    private String formDataSeparator;
    
    private String boundary;
    
    private boolean readyToPopulate;
    
    public RequestBodyWriterImpl() {
        this(null);
    }
    
    public RequestBodyWriterImpl(T back) {
        this.back = back;
        this.queryBuilder = new QueryParametersConverter("&");
        this.reset();
    }

    @Override
    public T back() {
        return Objects.requireNonNull(back);
    }
    
    @Override
    public RequestBodyWriter<T> reset() {
        this.clearCache();
        this.charset(StandardCharsets.UTF_8.name());
        this.formDataSeparator("\r\n");
        this.boundary(Long.toHexString(System.currentTimeMillis()));
        return this;
    }

    public RequestBodyWriter<T> clearCache() {
        this.paramBuffer.setLength(0);
        this.multiPartParamBuffer.setLength(0);
        this.resources.clear();
        this.readyToPopulate = true;
        return this;
    }
    
    public void makeReadyToPopulate() {
        if(!this.readyToPopulate) {
            this.clearCache();
        }
    }
    
    @Override
    public boolean hasOutput() {
        return this.hasMultiPartParameters() || 
                this.hasMultiPartResources() || 
                this.hasRequestParameters();
    }
    
    @Override
    public boolean hasMultiPartParameters() {
        return this.multiPartParamBuffer.length() > 0;
    }
    
    @Override
    public boolean hasMultiPartResources() {
        return !this.resources.isEmpty();
    }
    
    @Override
    public boolean hasRequestParameters() {
        return this.paramBuffer.length() > 0;
    }

    @Override
    public void write(OutputStream out) throws UnsupportedEncodingException, IOException {
        
        this.readyToPopulate = false;
        
        if(this.hasMultiPartParameters() || this.hasMultiPartResources()) {
            
            try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, this.charset), true)) {

                this.writeBuffer(writer, this.multiPartParamBuffer);

                for(Resource resource : this.resources) {

                    this.writeFormData(writer, out, resource, this.formDataSeparator);
                }

                // End of multipart/form-data.
                writer.append("--" + this.boundary + "--").append(this.formDataSeparator).flush();
            }
        }else if(this.hasRequestParameters()){
            
            try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, this.charset), true)) {

                this.writeBuffer(writer, this.paramBuffer);
            }
        }else{
            
            LOG.fine(() -> "Nothing to write");
        }
    }
    
    public boolean writeBuffer(PrintWriter writer, StringBuilder buff) {
        if(buff.length() > 0) {
            writer.write(buff.toString());
            writer.flush();
            return true;
        }else{
            return false;
        }
    }
    
    @Override
    public RequestBodyWriter<T> charset(String charset) {
        this.makeReadyToPopulate();
        this.charset = charset;
        return this;
    }

    @Override
    public RequestBodyWriter<T> formDataSeparator(String separator) {
        this.makeReadyToPopulate();
        this.formDataSeparator = separator;
        return this;
    }
    
    @Override
    public RequestBodyWriter<T> boundary(String boundary) {
        this.makeReadyToPopulate();
        this.boundary = boundary;
        return this;
    }
    
    @Override
    public RequestBodyWriter<T> param(String name, String value, boolean encode) {
        this.makeReadyToPopulate();
        this.appendParam(name, value, encode);
        return this;
    }

    @Override
    public RequestBodyWriter<T> multiPartParam(String name, String value) {
        this.makeReadyToPopulate();
        this.appendFormData(name, value, this.formDataSeparator);
        return this;
    }
    
    @Override
    public RequestBodyWriter<T> url(String name, URL url) {
        this.makeReadyToPopulate();
        return this.resource(new UrlResource(name, url));
    }
    
    @Override
    public RequestBodyWriter<T> file(String name, File file) {
        this.makeReadyToPopulate();
        return this.resource(new FileResource(name, file));
    }

    public RequestBodyWriter<T> resource(Resource resource) {
        this.makeReadyToPopulate();
        this.resources.add(resource);
        return this;
    }

    public boolean appendParam(String name, String value, boolean encode) {

        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        Objects.requireNonNull(charset);

        if(paramBuffer.length() > 0) {
            paramBuffer.append(this.queryBuilder.getSeparator());
        }
        
        final boolean appended = this.queryBuilder.appendQueryPair(
                name, value, paramBuffer, encode, this.charset);
        
        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Appended: {0}, {1} = {2}", new Object[]{appended, name, value});
        }
        
        return appended;
    }

    public void appendFormData(String name, Object value) {
        
        this.appendFormData(name, value, this.formDataSeparator);
    }
    
    public void appendFormData(String name, Object value, String separator) {
        
        LOG.finer(() -> "Writing multipart parameter: " + name + '=' + value);
        
        // Send normal param.
        multiPartParamBuffer.append("--" + boundary).append(separator);
        multiPartParamBuffer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(separator);
        multiPartParamBuffer.append("Content-Type: text/plain; charset=" + charset).append(separator);
        multiPartParamBuffer.append(separator);
        multiPartParamBuffer.append(value.toString()).append(separator);
    }

    public void writeFormData(PrintWriter writer, OutputStream output, Resource resource) 
            throws IOException {
        
        this.writeFormData(writer, output, resource, this.formDataSeparator);
    }

    public void writeFormData(PrintWriter writer, OutputStream output, Resource resource, String separator) 
            throws IOException {
        
        final String paramName = resource.getParamName();
        final String resourceName = resource.getResourceName();
        
        LOG.finer(() -> "Writing resource: " + paramName + '=' + resourceName);
        
        // Send binary file.
        writer.append("--" + boundary).append(separator);
        writer.append("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + resourceName + "\"").append(separator);
        writer.append("Content-Type: " + resource.getContentType("")).append(separator);
        writer.append("Content-Transfer-Encoding: binary").append(separator);
        writer.append(separator).flush();
        this.copyStream(resource.getInputStream(), output);
        // Important! Output cannot be closed. Close of writer will close output as well.
        this.flush(output);
        writer.append(separator).flush(); // CRLF is important! It indicates end of binary boundary.
    }

    private void flush(OutputStream c) {
        if (c != null) {
            try {
                c.flush();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Exception flushing.", e);
            }
        }
    }

    private long copyStream(InputStream from, OutputStream to) throws IOException {
        return this.copyStream(from, to, 0x800); // 2K chars (4K bytes) ;
    }
    
    /**
     * Copies all bytes from the input stream to the output stream.
     * Does not close or flush either stream.
     *
     * @param from the input stream to read from
     * @param to the output stream to write to
     * @param bufferSize The size of the buffer to put tye bytes in between read-write operation
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    private long copyStream(InputStream from, OutputStream to, int bufferSize) throws IOException {
        if (from == null || to == null) {
            throw new NullPointerException();
        }
        final byte[] buf = new byte[bufferSize];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public String getFormDataSeparator() {
        return formDataSeparator;
    }

    @Override
    public String getBoundary() {
        return boundary;
    }
}
