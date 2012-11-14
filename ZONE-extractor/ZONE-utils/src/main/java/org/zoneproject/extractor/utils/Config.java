package org.zoneproject.extractor.utils;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Desclaux Christophe <christophe@zouig.org>
 */
public class Config {
    private static Properties configFile = null;
    
    public static Properties getConfigFile(){
        if(configFile == null){
            try {
                configFile = new java.util.Properties();
                java.net.URL url = Config.class.getClassLoader().getResource("zone.properties");
                configFile.load(url.openStream());
            } catch (IOException ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return configFile;
    }
    
    public static String getVar(String var){
        return getConfigFile().getProperty(var);
    }
    
}
