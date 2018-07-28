package com.bc.net.cloudflare;

import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author USER
 * @see http://developers-club.com/posts/258101/
 */
public class CloudFlareResponseParameters extends HashMap {

    private transient static final Logger LOG = Logger.getLogger(CloudFlareResponseParameters.class.getName());
    
    private final static Pattern OPERATION_PATTERN = Pattern.compile("setTimeout\\(function\\(\\)\\{\\s+(var t,r,a,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n");
    private final static Pattern PASS_PATTERN = Pattern.compile("name=\"pass\" value=\"(.+?)\"");
    private final static Pattern CHALLENGE_PATTERN = Pattern.compile("name=\"jschl_vc\" value=\"(\\w+)\"");
    
    public CloudFlareResponseParameters() { }

    /**
     * @param url The URL of the page, which was redirected
     * @param responseText The response text
     * @throws javax.script.ScriptException
     */
    public void generate(URL url, String responseText) throws ScriptException {

        LOG.log(Level.FINER, "#generate(URL, String)");
        
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("JavaScript");

        LOG.log(Level.FINE, "Script Engine: {0}", engine);
        
        // вытаскиваем арифметику // Extract the arithmetics
        final Matcher operationSearch = OPERATION_PATTERN.matcher(responseText);
        final Matcher challengeSearch = CHALLENGE_PATTERN.matcher(responseText);
        final Matcher passSearch = PASS_PATTERN.matcher(responseText);
        if(!operationSearch.find() || !passSearch.find() || !challengeSearch.find()) {
            return; 
        }

        final String rawOperation = operationSearch.group(1); // операция
        final String challengePass = passSearch.group(1); // ключ
        final String challenge = challengeSearch.group(1); // хэш

        if(LOG.isLoggable(Level.FINE)) {        
            LOG.log(Level.FINE, "Raw operation: {0}, challenge pass: {1}, challenge: {2}", 
                    new Object[]{rawOperation, challengePass, challenge});
        }

        // вырезаем присвоение переменной
        final String operation = rawOperation
                .replaceAll("a\\.value =(.+?) \\+ .+?;", "$1")
                .replaceAll("\\s{3,}[a-z](?: = |\\.).+", "");
        
        final String js = operation.replace("\n", "");

        final int result = ((Double)engine.eval(js)).intValue();

        // Answer to the javascript challenge
        final String answer = String.valueOf(result + url.getHost().length()); 

        this.put("jschl_vc", challenge);
        this.put("pass", challengePass);
        this.put("jschl_answer", answer);

        LOG.log(Level.FINE, "Output parameters: {0}", CloudFlareResponseParameters.this);
    }
}
/**
 * 
    public void generate_old(URL url, String responseText) throws ScriptException {
        
        // инициализируем Rhino // Initialize the Rhino
        sun.org.mozilla.javascript.internal.Context rhino = sun.org.mozilla.javascript.internal.Context.enter();
        
        try {
            
            // вытаскиваем арифметику // Extract the arithmetics
            Matcher operationSearch = OPERATION_PATTERN.matcher(responseText);
            Matcher challengeSearch = CHALLENGE_PATTERN.matcher(responseText);
            Matcher passSearch = PASS_PATTERN.matcher(responseText);
            if(!operationSearch.find() || !passSearch.find() || !challengeSearch.find()) {
                return; 
            }
            
            String rawOperation = operationSearch.group(1); // операция
            String challengePass = passSearch.group(1); // ключ
            String challenge = challengeSearch.group(1); // хэш
            
XLogger.getInstance().log(Level.INFO, "Raw operation: {0}, challenge pass: {1}, challenge: {2}", 
        this.getClass(), rawOperation, challengePass, challenge);

            // вырезаем присвоение переменной
            String operation = rawOperation
                    .replaceAll("a\\.value =(.+?) \\+ .+?;", "$1")
                    .replaceAll("\\s{3,}[a-z](?: = |\\.).+", "");
            String js = operation.replace("\n", "");
            
            rhino.setOptimizationLevel(-1); // без этой строки rhino не запустится под Android
            sun.org.mozilla.javascript.internal.Scriptable scope = rhino.initStandardObjects(); // инициализируем пространство исполнения

            // either do or die trying
            int result = ((Double) rhino.evaluateString(scope, js, "CloudFlare JS Challenge", 1, null)).intValue();
            // ответ на javascript challenge // Answer to the javascript challenge
            String answer = String.valueOf(result + url.getHost().length()); 

            this.put("jschl_vc", challenge);
            this.put("pass", challengePass);
            this.put("jschl_answer", answer);

XLogger.getInstance().log(Level.INFO, "Output parameters: {0}", this.getClass(), CloudFlareResponseParameters.this);
            
        } finally {
            sun.org.mozilla.javascript.internal.Context.exit(); // выключаем Rhino // exit the Rhino
        }
    }
 * 
 */