/*
 * Copyright 2016 NUROX Ltd.
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Josh
 */
public class QueryParametersConverterTest {
    
    public QueryParametersConverterTest() { }
    
    @BeforeClass
    public static void setUpClass() { }
    
    @AfterClass
    public static void tearDownClass() { }
    
    @Before
    public void setUp() { }
    
    @After
    public void tearDown() { }

    @Test
    public void test() {
        this.test(false, false, "&", true);
        this.test(false, false, "&", false);
        this.test(true, true, "&amp;", true);
        this.test(true, true, "&amp;", false);
    }

    private void test(boolean nullsAllowed, boolean emptyStringsAllowed, String separator, boolean encodeOrDecode) {
        
System.out.println("#test{nullsAllowed="+nullsAllowed+", emptyStringsAllowed="+emptyStringsAllowed+", separator="+separator+"}");        

        final String charset = StandardCharsets.UTF_8.name();
        
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("A", "0^9");
        inputParams.put("B", "");
        inputParams.put("C", null);
        inputParams.put("D", "A to Z");
        inputParams.put("E", "\",");
System.out.println("Params: " + inputParams);        
        QueryParametersConverter instance = new QueryParametersConverter(nullsAllowed, emptyStringsAllowed, separator);
        
        final String query_1 = instance.convert(inputParams, encodeOrDecode, charset);
System.out.println(" Query: "+query_1);

        Map<String, String> outputParams_1 = instance.reverse(query_1, encodeOrDecode, charset);
System.out.println("Params: "+outputParams_1);

        instance = new QueryParametersConverter(nullsAllowed, emptyStringsAllowed, separator);

        final String query_2 = instance.convert(outputParams_1, encodeOrDecode, charset);
System.out.println(" Query: "+query_2);

        Map outputParams_2 = instance.reverse(query_2, encodeOrDecode, charset);
System.out.println("Params: "+outputParams_2);  

        assertEquals(query_1, query_2);
        
        assertEquals(outputParams_1, outputParams_2);
    }
}
