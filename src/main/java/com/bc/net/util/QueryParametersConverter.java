package com.bc.net.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @(#)QueryParametersConverter.java   25-Dec-2013 02:35:40
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
public class QueryParametersConverter implements Serializable {

    private static final Logger LOG = Logger.getLogger(QueryParametersConverter.class.getName());
    
    private final boolean emptyStringsAllowed;
    
    private final boolean nullsAllowed;
    
    /**
     * When there are multiple separators, the separator at this index
     * will be used to divide a pair.<br/>
     * Given the input: <tt>abc=1=d</tt><br/>
     * Considering the first '=' as separator we have. <tt>abc : 1=d</tt><br/>
     * Considering the second '=' as separator we have. <tt>abc=1 : d</tt><br/>
     * If this value is less than 0, then first separator will be used. If it is 
     * greater then the index of the last separator then the last separator will 
     * be used.
     */
    private final int separatorIndex;
    
    private final String separator;
    
    public QueryParametersConverter() { 
        this("&");
    }
    
    public QueryParametersConverter(String separator) { 
        this(false, separator);
    }
    
    public QueryParametersConverter(boolean emptyStringsAllowed, String separator) { 
        this(emptyStringsAllowed, false, 0, separator);
    }

    public QueryParametersConverter(
            boolean emptyStringsAllowed, boolean nullsAllowed, String separator) { 
        
        this(emptyStringsAllowed, nullsAllowed, 0, separator);
    }
    
    public QueryParametersConverter(
            boolean emptyStringsAllowed, boolean nullsAllowed, 
            int separatorIndex, String separator) { 
        this.emptyStringsAllowed = emptyStringsAllowed;
        this.nullsAllowed = nullsAllowed;
        this.separatorIndex = separatorIndex;
        this.separator = separator;
    }

    /**
     * @param queryString String of format 
     * <tt>key_0=val_0[SEPARATOR]key_1=val_1[SEPARATOR]... key_n=val_n</tt>
     * to convert into a map with corresponding key-value pairs. The default
     * value of <tt>[SEPARATOR]</tt> is '&'
     * @return The Map representation of the key-value pairs in the input query string.
     * @see #toMap(java.lang.String) 
     */
    public Map<String, String> reverse(String queryString) {
        return this.reverse(queryString, separator);
    }
    
    public Map<String, String> reverse(String queryString, boolean decode, String charset) {
        return this.toMap(queryString, separator, decode, charset);
    }
    
    public Map<String, String> reverse(String queryString, String separator) {
        return this.toMap(queryString, separator, false, "UTF-8");
    }
    
    /**
     * @param queryString String of format 
     * <tt>key_0=val_0[SEPARATOR]key_1=val_1[SEPARATOR]... key_n=val_n</tt>
     * to convert into a map with corresponding key-value pairs.
     * @param separator The separator between query pairs. The default is <tt>'&'</tt>
     * @param decode 
     * @param charset 
     * @return The Map representation of the key-value pairs in the input query string.
     */
    public Map<String, String> toMap(String queryString, String separator, boolean decode, String charset) {
        
        if(queryString == null) {
            throw new NullPointerException();
        }
        
        if(queryString.startsWith("?")) {
            queryString = queryString.substring(1);
        }
        
        if(LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, 
                    "Separator {0}. nulls allowed: {1}, empty strings allowed: {2}, query: {3}", 
                    new Object[]{separator, nullsAllowed, emptyStringsAllowed, queryString}); 
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<>();

        String [] queryPairs = queryString.split(separator);

        for(int i=0; i<queryPairs.length; i++) {
            
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Pair[{0}]: {1}", new Object[]{i, queryPairs[i]});
            }

            String [] parts = queryPairs[i].split("=");
            
            String key;
            String val;
            
            if(parts == null || parts.length == 0) {
                continue;
            }else if (parts.length == 1) {
                if(this.emptyStringsAllowed) {
                    key = parts[0];
                    val = ""; //We prefer an empty String to null -> Query standards
                }else{
                    continue;
                }
            }else if(parts.length == 2) {
                key = parts[0];
                val = parts[1];
            }else{
                
                final int index = separatorIndex < 0 ? 0 : separatorIndex >= parts.length-1 ? parts.length - 2 : separatorIndex;

                StringBuilder builder = new StringBuilder();
                for(int partIndex=0; partIndex<index+1; partIndex++) {
                    builder.append(parts[partIndex]);
                    if(partIndex < index) {
                        builder.append('=');
                    }
                }
                key = builder.toString();
                
                builder.setLength(0);
                for(int partIndex=index+1; partIndex<parts.length; partIndex++) {
                    builder.append(parts[partIndex]);
                    if(partIndex < parts.length-1) {
                        builder.append('=');
                    }
                }
                val = builder.toString();
            }
            
            key = key.trim();
            val = val == null ? null : val.trim();
            
            result.put(this.reverseKey(key, val), this.reverseValue(key, val, decode, charset));
        }
        
        LOG.log(Level.FINER, "Output: {0}", result);        

        return result;
    }
    
    public String reverseKey(String key, String value) {
        return key;
    }
    
    public String reverseValue(String key, String val, boolean decode, String charset) {
        if(val != null && charset != null && decode) {
            val = (String)this.urlDecode(val, charset);
        }
        return val;
    }
    
    public String convert(Map params) {
        return this.convert(params, false);
    }
    
    public String convert(Map params, boolean encode) {
        return this.toQueryString(params, encode, StandardCharsets.UTF_8.name());
    }
    
    public String convert(Map params, boolean encode, String charset) {
        return this.toQueryString(params, encode, charset);
    }
    /**
     * Converts the key/value pairs in the input map to a query string
     * for the format <tt>key_0=val_0&key_1=val_1...</tt>
     * @param params The Map whose key/value pairs will be converted to a
     * query String.
     * @return The query string representation of the input Map. 
     */
    public String toQueryString(Map params, boolean encode, String charset) {
        
        StringBuilder builder = new StringBuilder();
        
        this.appendQueryString(params, builder, encode, charset);
        
        return builder.toString();
    }

    public int appendQueryString(Map params, StringBuilder appendTo, boolean encode, String charset) {

        Iterator iter = params.entrySet().iterator();
        
        int appended = 0;
        
        while(iter.hasNext()) {
            
            final java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
            final Object key = entry.getKey();
            final Object val = entry.getValue();
            
            final String prefix = appended > 0 ? separator : null;
            
            if(this.appendQueryPair(prefix, key, val, appendTo, encode, charset)) {
                ++appended;
            }
        }
        
        return appended;
    }

    public boolean appendQueryPair(Object key, Object val, StringBuilder appendTo, boolean encode, String charset) {
        return this.appendQueryPair(null, key, val, appendTo, encode, charset);
    }
    
    protected boolean appendQueryPair(String prefix, Object key, Object val, StringBuilder appendTo, boolean encode, String charset) {

        if(!this.emptyStringsAllowed && val instanceof String && ((String)val).isEmpty()) {
            return false;
        }

        if(!this.nullsAllowed && val == null) {
            return false;
        }

        if(prefix != null) {
            appendTo.append(prefix);
        }
        appendTo.append(this.convertKey(key, val));
        appendTo.append('=');
        appendTo.append(this.convertValue(key, val, encode, charset));
        
        return true;
    }

    public Object convertKey(Object key, Object val) {
        return key;
    }
    
    public Object convertValue(Object key, Object val, boolean encode, String charset) {
        if(val != null && charset != null && encode) {
            val = this.urlEncode(val, charset);
        }
        return val;
    }

    /**
     * @param val The value to URL encode
     * @param charset The name of the charset to use in URL encoding the input value
     * @return The URL encoded value
     * @throws java.lang.RuntimeException if {@link java.io.UnsupportedEncodingException} is thrown
    */
    public Object urlEncode(Object val, String charset) {
        try {
            val = val == null ? null : URLEncoder.encode(val.toString(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return val;
    }

    /**
     * @param val The value to URL decode
     * @param charset The name of the charset to use in URL encoding the input value
     * @return The URL decoded value
     * @throws java.lang.RuntimeException if {@link java.io.UnsupportedEncodingException} is thrown
    */
    public Object urlDecode(Object val, String charset) {
        try {
            val = val == null ? null : URLDecoder.decode(val.toString(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return val;
    }

    public final int getSeparatorIndex() {
        return separatorIndex;
    }

    public final boolean isEmptyStringsAllowed() {
        return emptyStringsAllowed;
    }

    public final boolean isNullsAllowed() {
        return nullsAllowed;
    }

    public final String getSeparator() {
        return separator;
    }
}
