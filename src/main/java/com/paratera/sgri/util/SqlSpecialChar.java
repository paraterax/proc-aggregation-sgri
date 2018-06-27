package com.paratera.sgri.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sql特殊字符.
 */
public final class SqlSpecialChar {
    /**
     * 特殊字符缓存
     */
    private static Map<String, Map<String, String>> charCache = new HashMap<String, Map<String, String>>();

    static {
        Map<String, String> readCharMap = new HashMap<String, String>() {
            private static final long serialVersionUID = -8159217963927014181L;
            {
                put("\\", "\\\\\\\\");
                put("'", "''");
                put("%", "\\%");
                put("_", "\\_");
            }
        };
        charCache.put("read", readCharMap);
        Map<String, String> writeCharMap = new HashMap<String, String>() {
            private static final long serialVersionUID = -2537530523792264151L;
            {
                put("\\", "\\\\");
                put("'", "''");
                put("%", "\\%");
                put("_", "\\_");
            }
        };
        charCache.put("write", writeCharMap);
    }

    /**
     * 默认构造函数.
     */
    private SqlSpecialChar() {
    }

    /**
     * 替换Sql特殊字符.
     */
    public static String replace(final String sqlValue, String mode) {
        String ret = sqlValue;
        if (ret != null) {
            Map<String, String> charMap = charCache.get(mode);
            if (charMap != null) {
                Iterator<Entry<String, String>> it = charMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> entry = it.next();
                    ret = ret.replace(entry.getKey(), entry.getValue());
                }
            }
        }

        return ret;
    }
}
