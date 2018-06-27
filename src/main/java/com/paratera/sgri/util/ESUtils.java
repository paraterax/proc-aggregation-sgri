package com.paratera.sgri.util;


import com.paratera.sgri.config.ConfigParams;
import com.paratera.sgri.http.HttpClient;
import com.paratera.sgri.http.HttpResponse;

public class ESUtils {
    private static final HttpClient CLIENT = HttpClient.getInstance();


    public static boolean isIndexExists(String indexName) {
        boolean exists = false;
        String indexUrl = ConfigParams.ES_SERVER + "/" + indexName + "/_search?size=0";
        HttpResponse rd = CLIENT.get(indexUrl);
        if (rd.isSuccess()) {
            if (rd.getData().indexOf("index_not_found_exception") > 0) {
                exists = false;
            } else {
                exists = true;
            }
        }
        return exists;
    }

    public static boolean isIndexClosed(String indexName) {
        boolean closed = false;
        String indexUrl = ConfigParams.ES_SERVER + "/" + indexName + "/_search?size=0";
        HttpResponse rd = CLIENT.get(indexUrl);
        if (rd.isSuccess()) {
            if (rd.getData().indexOf("index_closed_exception") > 0) {
                closed = true;
            }
        }
        return closed;
    }

    public static boolean openIndex(String indexName) {
        return toggleIndex(indexName, "_open");
    }

    public static boolean closeIndex(String indexName) {
        return toggleIndex(indexName, "_close");
    }

    /**
     * 关闭或打开索引ConfigParams.ES_SERVER
     */
    private static boolean toggleIndex(String indexName, String operation) {
        boolean ret = false;
        String url = ConfigParams.ES_SERVER + "/" + indexName + "/" + operation;
        HttpResponse rd = CLIENT.post(url, null);
        if (rd.isSuccess() && rd.getStatusCode() == 200) {
            ret = true;
        }
        return ret;
    }

}
