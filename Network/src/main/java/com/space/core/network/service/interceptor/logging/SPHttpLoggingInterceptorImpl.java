package com.space.core.network.service.interceptor.logging;

import androidx.annotation.NonNull;

import com.space.core.common.logger.SPLogger;
import com.space.core.network.helper.logger.SPSentryHttpLogger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * {@linkplain OkHttpClient#interceptors() application interceptor} or as a {@linkplain
 * OkHttpClient#networkInterceptors() network interceptor}. <p> The format of the logs created by
 * this class should not be considered stable and may change slightly between releases. If you need
 * a stable logging format, use your own interceptor.
 */
public final class SPHttpLoggingInterceptorImpl implements SPLoggingInterceptor {
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * }</pre>
         */
        HEADERS,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    private static final String REQUEST_PREFIX = "Network Request --> ";
    private static final String RESPONSE_PREFIX = "Network Response <-- ";

    private void log(String message) {
        logger.logMessage(message);
    }

    public SPHttpLoggingInterceptorImpl(SPLogger logger, SPSentryHttpLogger sentryLogger) {
        this.logger = logger;
        this.sentryLogger = sentryLogger;
    }

    private final SPLogger logger;
    private final SPSentryHttpLogger sentryLogger;

    private volatile Set<String> headersToRedact = Collections.emptySet();

    public void redactHeader(String name) {
        Set<String> newHeadersToRedact = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        newHeadersToRedact.addAll(headersToRedact);
        newHeadersToRedact.add(name);
        headersToRedact = newHeadersToRedact;
    }

    private volatile Level level = Level.NONE;

    /**
     * Change the level at which this interceptor logs.
     */
    public SPHttpLoggingInterceptorImpl setLevel(Level level) {
        if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
        this.level = level;
        return this;
    }

    public Level getLevel() {
        return level;
    }

    @Override
    @NonNull
    public Response intercept(Chain chain) throws IOException {
        Level level = this.level;

        Request request = chain.request();
        if (level == Level.NONE) {
            return chain.proceed(request);
        }

        boolean logBody = level == Level.BODY;
        boolean logHeaders = logBody || level == Level.HEADERS;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        log("");
        log("<<<<<<<<<<<<<<<<<<<< Request >>>>>>>>>>>>>>>>>>>>");
        Connection connection = chain.connection();
        String requestStartMessage = REQUEST_PREFIX
            + request.method()
            + ' ' + request.url()
            + (connection != null ? " " + connection.protocol() : "");
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }

        log(requestStartMessage);

        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null) {
                    log(REQUEST_PREFIX + "Content-Type: " + requestBody.contentType());
                }
                if (requestBody.contentLength() != -1) {
                    log(REQUEST_PREFIX + "Content-Length: " + requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    logHeader(REQUEST_PREFIX, headers, i);
                }
            }

            if (!logBody || !hasRequestBody) {
                log(REQUEST_PREFIX + "END " + request.method());
            } else if (bodyHasUnknownEncoding(request.headers())) {
                log(REQUEST_PREFIX + "END " + request.method() + " (encoded body omitted)");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                log("");
                assert charset != null;
                sentryLogger.captureHttpRequestData(buffer.clone().readString(charset));
                if (isPlaintext(buffer)) {
                    log(buffer.clone().readString(charset));
                    log(REQUEST_PREFIX + "END " + request.method()
                        + " (" + requestBody.contentLength() + "-byte body)");
                } else {
                    log(REQUEST_PREFIX + "END " + request.method() + " (binary "
                        + requestBody.contentLength() + "-byte body omitted)");
                }
            }
        }
        log("");
        log(">>>>>>>>>>>>>>>>>>>> Response <<<<<<<<<<<<<<<<<<<<");
        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            log(RESPONSE_PREFIX + "HTTP FAILED: " + e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        assert responseBody != null;
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        log(RESPONSE_PREFIX
                + response.code()
                + (response.message().isEmpty() ? "" : ' ' + response.message())
                + ' ' + response.request().url()
                + " (" + tookMs + "ms" + (!logHeaders ? ", " + bodySize + " body" : "") + ')');

        if (logHeaders) {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                logHeader(RESPONSE_PREFIX, headers, i);
            }

            if (!logBody || !HttpHeaders.hasBody(response)) {
                log(RESPONSE_PREFIX + "END HTTP");
            } else if (bodyHasUnknownEncoding(response.headers())) {
                log(RESPONSE_PREFIX + "END HTTP (encoded body omitted)");
            } else {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();

                Long gzippedLength = null;
                if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
                    gzippedLength = buffer.size();
                    GzipSource gzippedResponseBody = null;
                    try {
                        gzippedResponseBody = new GzipSource(buffer.clone());
                        buffer = new Buffer();
                        buffer.writeAll(gzippedResponseBody);
                    } finally {
                        if (gzippedResponseBody != null) {
                            gzippedResponseBody.close();
                        }
                    }
                }

                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                if (!isPlaintext(buffer)) {
                    log(RESPONSE_PREFIX + "END HTTP (binary " + buffer.size() + "-byte body omitted)");
                    return response;
                }

                if (contentLength != 0) {
                    assert charset != null;
                    log(RESPONSE_PREFIX + buffer.clone().readString(charset));
                    sentryLogger.captureHttpResponseData(buffer.clone().readString(charset));
                }

                if (gzippedLength != null) {
                    log(RESPONSE_PREFIX + "END HTTP (" + buffer.size() + "-byte, "
                        + gzippedLength + "-gzipped-byte body)");
                } else {
                    log(RESPONSE_PREFIX + "END HTTP (" + buffer.size() + "-byte body)");
                }
            }
        }

        return response;
    }

    private void logHeader(String logPrefix, Headers headers, int i) {
        String value = headersToRedact.contains(headers.name(i)) ? "██" : headers.value(i);
        log(logPrefix + headers.name(i) + ": " + value);
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private static boolean bodyHasUnknownEncoding(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null
            && !contentEncoding.equalsIgnoreCase("identity")
            && !contentEncoding.equalsIgnoreCase("gzip");
    }
}
