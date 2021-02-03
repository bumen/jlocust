package com.github.qvp.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @date 2021-02-03
 * @author zhangyuqiang02@playcrab.com
 */
public abstract class PropertyUtils {


    public static PropertiesHelper readPropertiesFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.notExists(path)) {
            throw new IllegalStateException("error read properties file " + filePath);
        }

        try (InputStream in = Files.newInputStream(path)) {
            Properties properties = new Properties();
            properties.load(in);

            PropertiesHelper helper = new PropertiesHelper(properties);

            return helper;
        } catch (Exception e) {
            throw e;
        }
    }


    public static class PropertiesHelper {
        private Properties properties;

        public PropertiesHelper(Properties properties) {
            this.properties = properties;
        }

        public String getString(String key) {
            return this.properties.getProperty(key);
        }

        public String getString(String key, String def) {
            String v = properties.getProperty(key);
            if (v == null) {
                return def;
            }

            return v;
        }

        public int getIntValue(String key) {
            Object value = properties.get(key);
            Integer intVal = castToInt(value);
            return intVal == null ? 0 : intVal;
        }

        public int getIntValue(String key, int def) {
            Object value = properties.get(key);
            Integer intVal = castToInt(value);
            if (intVal == null) {
                return def;
            }

            return intVal;
        }
    }

    public static Integer castToInt(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer)value;
        } else if (value instanceof Number) {
            return ((Number)value).intValue();
        } else if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.length() != 0 && !"null".equals(strVal) && !"NULL".equals(strVal)) {
                if (strVal.indexOf(44) != 0) {
                    strVal = strVal.replaceAll(",", "");
                }

                return Integer.parseInt(strVal);
            } else {
                return null;
            }
        } else if (value instanceof Boolean) {
            return (Boolean)value ? 1 : 0;
        } else {

            throw new IllegalStateException("can not cast to int, value : " + value);
        }
    }

}
