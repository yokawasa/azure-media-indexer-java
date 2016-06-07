package azuremediaindexer;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class PropertyLoader {

    private static PropertyLoader instance;
    private Properties prop;

    public static PropertyLoader getInstance(String conffile) throws IOException {
        instance = new PropertyLoader(conffile);
        return instance;
    }

    public PropertyLoader(String conffile) throws IOException {
        FileInputStream fis = new FileInputStream(new File(conffile));
        this.prop = new Properties();
        this.prop.load(fis);
        fis.close();
    }

    public String getValue(String key){
        return this.prop.getProperty(key);
    }
}

