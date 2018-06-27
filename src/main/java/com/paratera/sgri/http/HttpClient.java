package com.paratera.sgri.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paratera.sgri.config.ConfigParams;

/**
 * 一句话描述这个类的作用.
 */
public final class HttpClient {
    /**
     * 日志实例.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    private volatile static HttpClient ins = null;

    private static PoolingHttpClientConnectionManager POOL = null;

    private static CloseableHttpClient CLIENT = null;

    /**
     * 默认构造函数.
     */
    private HttpClient() {
        POOL = this.getHttpPool();
        CLIENT = this.getHttpClient();
    }

    public static HttpClient getInstance() {
        if (ins == null) {
            synchronized (HttpClient.class) {
                if (ins == null) {
                    ins = new HttpClient();
                }
            }
        }
        return ins;
    }

    public void shutdown() throws IOException {
        CLIENT.close();
        POOL.close();
    }

    static enum HttpMethod {
        GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE
    }

    public HttpResponse get(String getUrl) {
        return method(getUrl, HttpClient.HttpMethod.GET);
    }

    public HttpResponse post(String postUrl, String postData) {
        return method(postUrl, HttpClient.HttpMethod.POST, postData);
    }

    public HttpResponse delete(String deleteUrl) {
        return method(deleteUrl, HttpClient.HttpMethod.DELETE);
    }

    private HttpResponse method(String url, HttpClient.HttpMethod method, String... postData) {
        HttpRequestBase request = null;
        switch (method) {
            case GET: {
                request = new HttpGet(url);
                break;
            }
            case POST: {
                request = new HttpPost(url);
                break;
            }
            case DELETE: {
                request = new HttpDelete(url);
                break;
            }
            default: {
                request = new HttpGet(url);
                break;
            }
        }

        HttpResponse rd = new HttpResponse();
        rd.setUrl(url);
        if (postData != null && postData.length > 0) {
            rd.setParams(postData[0]);
        }

        String errorMsg = null;
        String rspData = null;
        int statusCode = 0;
        CloseableHttpResponse response = null;
        try {
            configRequestHeader(request);
            if (postData.length > 0 && postData[0] != null) {
                StringEntity stringEntity = new StringEntity(postData[0], "utf-8");
                ((HttpPost) request).setEntity(stringEntity);
            }
            response = CLIENT.execute(request, HttpClientContext.create());
            statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            rspData = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
        } catch (IOException e) {
            errorMsg = e.toString() + ": " + url;
            LOGGER.error(errorMsg, e);
        } catch (Exception e) {
            errorMsg = e.toString() + ": " + url;
            LOGGER.error(errorMsg, e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }

        rd.setStatusCode(statusCode);
        rd.setErrorMsg(errorMsg);
        rd.setData(rspData);
        return rd;
    }

    private void configRequestHeader(HttpRequestBase request) {
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:49.0) Gecko/20100101 Firefox/49.0");
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");// "en-US,en;q=0.5");
        request.setHeader("Accept-Charset", "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(ConfigParams.CONN_MANAGER_TIMEOUT).setConnectTimeout(ConfigParams.CONNECT_TIMEOUT)
                .setSocketTimeout(ConfigParams.SO_TIMEOUT).build();
        request.setConfig(requestConfig);
    }

    private PoolingHttpClientConnectionManager getHttpPool() {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", plainsf).register("https", sslsf).build();
        PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry);
        pool.setMaxTotal(ConfigParams.MAX_TOTAL_CONNECTIONS);
        pool.setDefaultMaxPerRoute(pool.getMaxTotal());
        return pool;
    }

    private CloseableHttpClient getHttpClient() {
        return HttpClients.custom().setConnectionManager(POOL).setRetryHandler(getHRRHandler()).build();
    }

    private HttpRequestRetryHandler getHRRHandler() {
        return new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 3) {
                    // 如果已经执行了3次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {
                    // 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    // 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    // 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {
                    // 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {
                    // ssl握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
    }
}
