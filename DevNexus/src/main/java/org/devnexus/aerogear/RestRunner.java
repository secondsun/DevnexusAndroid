/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devnexus.aerogear;

import android.util.Log;
import android.util.Pair;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.http.HttpStatus;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.ReadFilter;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.pipeline.GsonRequestBuilder;
import org.jboss.aerogear.android.impl.pipeline.GsonResponseParser;
import org.jboss.aerogear.android.impl.pipeline.PipeConfig;
import org.jboss.aerogear.android.impl.pipeline.paging.DefaultParameterProvider;
import org.jboss.aerogear.android.impl.pipeline.paging.URIBodyPageParser;
import org.jboss.aerogear.android.impl.pipeline.paging.URIPageHeaderParser;
import org.jboss.aerogear.android.impl.pipeline.paging.WebLink;
import org.jboss.aerogear.android.impl.pipeline.paging.WrappingPagedList;
import org.jboss.aerogear.android.impl.reflection.Property;
import org.jboss.aerogear.android.impl.reflection.Scan;
import org.jboss.aerogear.android.impl.util.ParseException;
import org.jboss.aerogear.android.impl.util.UrlUtils;
import org.jboss.aerogear.android.impl.util.WebLinkParser;
import org.jboss.aerogear.android.pipeline.Pipe;
import org.jboss.aerogear.android.pipeline.PipeHandler;
import org.jboss.aerogear.android.pipeline.RequestBuilder;
import org.jboss.aerogear.android.pipeline.ResponseParser;
import org.jboss.aerogear.android.pipeline.paging.PageConfig;
import org.jboss.aerogear.android.pipeline.paging.ParameterProvider;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RestRunner<T> implements PipeHandler<T> {

    private final static Set<Integer> RETRY_CODES = Sets.newHashSet(HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_FORBIDDEN);
    private final PageConfig pageConfig;
    private static final String TAG = RestRunner.class.getSimpleName();
    private final RequestBuilder<T> requestBuilder;
    private final String dataRoot;
    private final ParameterProvider parameterProvider;
    /**
     * A class of the Generic type this pipe wraps. This is used by GSON for
     * deserializing.
     */
    private final Class<T> klass;
    /**
     * A class of the Generic collection type this pipe wraps. This is used by
     * JSON for deserializing collections.
     */
    private final Class<T[]> arrayKlass;
    private final URL baseURL;
    private final Provider<HttpProvider> httpProviderFactory = new HttpProviderFactory();
    private final Integer timeout;
    private final ResponseParser<T> responseParser;
    private AuthenticationModule authModule;
    private Charset encoding = Charset.forName("UTF-8");

    public RestRunner(Class<T> klass, URL baseURL) {
        this.klass = klass;
        this.arrayKlass = asArrayClass(klass);
        this.baseURL = baseURL;
        this.dataRoot = "";
        this.requestBuilder = new GsonRequestBuilder<T>();
        this.pageConfig = null;
        this.parameterProvider = new DefaultParameterProvider();
        this.timeout = 60000;
        this.responseParser = new GsonResponseParser<T>();
    }

    public RestRunner(Class<T> klass, URL baseURL,
                      PipeConfig config) {
        this.klass = klass;
        this.arrayKlass = asArrayClass(klass);
        this.baseURL = baseURL;
        this.timeout = config.getTimeout();

        if (config.getRequestBuilder() != null) {
            this.requestBuilder = config.getRequestBuilder();
        } else {
            this.requestBuilder = new GsonRequestBuilder<T>();
        }

        if (config.getEncoding() != null) {
            this.encoding = config.getEncoding();
        } else {
            this.encoding = Charset.forName("UTF-8");
        }

        if (config.getDataRoot() != null) {
            this.dataRoot = config.getDataRoot();
        } else {
            this.dataRoot = "";
        }

        if (config.getResponseParser() != null) {
            this.responseParser = config.getResponseParser();
        } else {
            this.responseParser = new GsonResponseParser<T>();
        }

        if (config.getPageConfig() != null) {
            this.pageConfig = config.getPageConfig();

            if (pageConfig.getParameterProvider() != null) {
                this.parameterProvider = pageConfig.getParameterProvider();
            } else {
                this.parameterProvider = new DefaultParameterProvider();
            }

            if (pageConfig.getPageParameterExtractor() == null) {
                if (PageConfig.MetadataLocations.BODY.equals(pageConfig.getMetadataLocation())) {
                    pageConfig.setPageParameterExtractor(new URIBodyPageParser(baseURL));
                } else if (PageConfig.MetadataLocations.HEADERS.equals(pageConfig.getMetadataLocation())) {
                    pageConfig.setPageParameterExtractor(new URIPageHeaderParser(baseURL));
                }
            }

        } else {
            this.pageConfig = null;
            this.parameterProvider = new DefaultParameterProvider();
        }

        if (config.getAuthModule() != null) {
            this.authModule = config.getAuthModule();
        }

    }

    @Override
    public List<T> onRead(Pipe<T> requestingPipe) {
        return onReadWithFilter(new ReadFilter(), requestingPipe);
    }

    @Override
    public T onSave(T data) {

        final String id;
        String recordIdFieldName = Scan.recordIdFieldNameIn(data.getClass());
        Object idObject = new Property(data.getClass(), recordIdFieldName).getValue(data);
        id = idObject == null ? null : idObject.toString();

        byte[] body = requestBuilder.getBody(data);
        final HttpProvider httpProvider = getHttpProvider();

        HeaderAndBody result;
        if (id == null || id.length() == 0) {
            try {
                result = httpProvider.post(body);
            } catch (HttpException exception) {
                if (RETRY_CODES.contains(exception.getStatusCode()) && retryAuth(authModule)) {
                    result = httpProvider.post(body);
                } else {
                    throw exception;
                }
            }

        } else {
            try {
                result = httpProvider.put(id, body);
            } catch (HttpException exception) {
                if (RETRY_CODES.contains(exception.getStatusCode()) && retryAuth(authModule)) {
                    result = httpProvider.put(id, body);
                } else {
                    throw exception;
                }
            }

        }

        return responseParser.handleResponse(new String(result.getBody(), encoding), klass);
    }

    @Override
    public List<T> onReadWithFilter(ReadFilter filter, Pipe<T> requestingPipe) {
        List<T> result;
        HttpProvider httpProvider;

        if (filter == null) {
            filter = new ReadFilter();
        }

        if (filter.getLinkUri() == null) {
            httpProvider = getHttpProvider(parameterProvider.getParameters(filter));
        } else {
            httpProvider = getHttpProvider(filter.getLinkUri());
        }

        HeaderAndBody httpResponse;

        try {
            httpResponse = httpProvider.get();
        } catch (HttpException exception) {
            if (RETRY_CODES.contains(exception.getStatusCode()) && retryAuth(authModule)) {
                httpResponse = httpProvider.get();
            } else {
                throw exception;
            }
        }
        byte[] responseBody = httpResponse.getBody();
        String responseAsString = new String(responseBody, encoding);
        JsonParser parser = new JsonParser();
        JsonElement httpJsonResult = parser.parse(responseAsString);
        httpJsonResult = getResultElement(httpJsonResult, dataRoot);
        if (httpJsonResult.isJsonArray()) {
            T[] resultArray = responseParser.handleArrayResponse(httpJsonResult.toString(), arrayKlass);
            result = Arrays.asList(resultArray);
            if (pageConfig != null) {
                result = computePagedList(result, httpResponse, filter.getWhere(), requestingPipe);
            }
        } else {
            T resultObject = responseParser.handleResponse(httpJsonResult.toString(), klass);
            List<T> resultList = new ArrayList<T>(1);
            resultList.add(resultObject);
            result = resultList;
            if (pageConfig != null) {
                result = computePagedList(result, httpResponse, filter.getWhere(), requestingPipe);
            }
        }
        return result;

    }

    @Override
    public void onRemove(String id) {
        HttpProvider httpProvider = getHttpProvider();
        try {
            httpProvider.delete(id);
        } catch (HttpException exception) {
            if (RETRY_CODES.contains(exception.getStatusCode()) && retryAuth(authModule)) {
                httpProvider.delete(id);
            } else {
                throw exception;
            }
        }

    }

    /**
     * This will return a class of the type T[] from a given class. When we read
     * from the AG pipe, Java needs a reference to a generic array type.
     *
     * @param klass
     * @return an array of klass with a length of 1
     */
    private Class<T[]> asArrayClass(Class<T> klass) {
        return (Class<T[]>) Array.newInstance(klass, 1).getClass();
    }

    /**
     * @param queryParameters
     * @return a url with query params added
     */
    private URL addAuthorization(List<Pair<String, String>> queryParameters, URL baseURL) {

        StringBuilder queryBuilder = new StringBuilder();

        String amp = "";
        for (Pair<String, String> parameter : queryParameters) {
            try {
                queryBuilder.append(amp)
                        .append(URLEncoder.encode(parameter.first, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(parameter.second, "UTF-8"));

                amp = "&";
            } catch (UnsupportedEncodingException ex) {
                Log.e(TAG, "UTF-8 encoding is not supported.", ex);
                throw new RuntimeException(ex);

            }
        }

        return appendQuery(queryBuilder.toString(), baseURL);

    }

    private void addAuthHeaders(HttpProvider httpProvider, AuthorizationFields fields) {
        List<Pair<String, String>> authHeaders = fields.getHeaders();

        for (Pair<String, String> header : authHeaders) {
            httpProvider.setDefaultHeader(header.first, header.second);
        }

    }

    private HttpProvider getHttpProvider() {
        return getHttpProvider(URI.create(""));
    }

    private HttpProvider getHttpProvider(URI relativeUri) {
        final String queryString;

        AuthorizationFields fields = loadAuth(relativeUri, "GET");

        if (relativeUri == null || relativeUri.getQuery() == null) {
            queryString = "";
        } else {
            queryString = relativeUri.getQuery().toString();
        }

        URL mergedURL = UrlUtils.appendToBaseURL(baseURL, relativeUri.getPath());
        URL authorizedURL = addAuthorization(fields.getQueryParameters(), UrlUtils.appendQueryToBaseURL(mergedURL, queryString));

        final HttpProvider httpProvider = httpProviderFactory.get(authorizedURL, timeout);
        httpProvider.setDefaultHeader("Content-TYpe", requestBuilder.getContentType());
        addAuthHeaders(httpProvider, fields);
        return httpProvider;

    }

    /**
     * Apply authentication if the token is present
     */
    private AuthorizationFields loadAuth(URI relativeURI, String httpMethod) {

        if (authModule != null && authModule.isLoggedIn()) {
            return authModule.getAuthorizationFields(relativeURI, httpMethod, new byte[]{});
        }

        return new AuthorizationFields();
    }

    /**
     * This method checks for paging information and returns the appropriate
     * data
     *
     * @param result
     * @param httpResponse
     * @param where
     * @return a {@link WrappingPagedList} if there is paging, result if not.
     */
    private List<T> computePagedList(List<T> result, HeaderAndBody httpResponse, JSONObject where, Pipe<T> requestingPipe) {
        ReadFilter previousRead = null;
        ReadFilter nextRead = null;

        if (PageConfig.MetadataLocations.WEB_LINKING.equals(pageConfig.getMetadataLocation())) {
            String webLinksRaw = "";
            final String relHeader = "rel";
            final String nextIdentifier = pageConfig.getNextIdentifier();
            final String prevIdentifier = pageConfig.getPreviousIdentifier();
            try {
                webLinksRaw = getWebLinkHeader(httpResponse);
                if (webLinksRaw == null) { //no paging, return result
                    return result;
                }
                List<WebLink> webLinksParsed = WebLinkParser.parse(webLinksRaw);
                for (WebLink link : webLinksParsed) {
                    if (nextIdentifier.equals(link.getParameters().get(relHeader))) {
                        nextRead = new ReadFilter();
                        nextRead.setLinkUri(new URI(link.getUri()));
                    } else if (prevIdentifier.equals(link.getParameters().get(relHeader))) {
                        previousRead = new ReadFilter();
                        previousRead.setLinkUri(new URI(link.getUri()));
                    }

                }
            } catch (URISyntaxException ex) {
                Log.e(TAG, webLinksRaw + " did not contain a valid context URI", ex);
                throw new RuntimeException(ex);
            } catch (ParseException ex) {
                Log.e(TAG, webLinksRaw + " could not be parsed as a web link header", ex);
                throw new RuntimeException(ex);
            }
        } else if (pageConfig.getMetadataLocation().equals(PageConfig.MetadataLocations.HEADERS)) {
            nextRead = pageConfig.getPageParameterExtractor().getNextFilter(httpResponse, RestRunner.this.pageConfig);
            previousRead = pageConfig.getPageParameterExtractor().getPreviousFilter(httpResponse, RestRunner.this.pageConfig);
        } else if (pageConfig.getMetadataLocation().equals(PageConfig.MetadataLocations.BODY)) {
            nextRead = pageConfig.getPageParameterExtractor().getNextFilter(httpResponse, RestRunner.this.pageConfig);
            previousRead = pageConfig.getPageParameterExtractor().getPreviousFilter(httpResponse, RestRunner.this.pageConfig);
        } else {
            throw new IllegalStateException("Not supported");
        }
        if (nextRead != null) {
            nextRead.setWhere(where);
        }

        if (previousRead != null) {
            previousRead.setWhere(where);
        }

        return new WrappingPagedList<T>(requestingPipe, result, nextRead, previousRead);
    }

    private String getWebLinkHeader(HeaderAndBody httpResponse) {
        String linkHeaderName = "Link";
        Object header = httpResponse.getHeader(linkHeaderName);
        if (header != null) {
            return header.toString();
        }
        return null;
    }

    public void setAuthenticationModule(AuthenticationModule module) {
        this.authModule = module;
    }

    private URL appendQuery(String query, URL baseURL) {
        try {
            URI baseURI = baseURL.toURI();
            String baseQuery = baseURI.getQuery();
            if (baseQuery == null || baseQuery.isEmpty()) {
                baseQuery = query;
            } else {
                if (query != null && !query.isEmpty()) {
                    baseQuery = baseQuery + "&" + query;
                }
            }

            if (baseQuery.isEmpty()) {
                baseQuery = null;
            }

            return new URI(baseURI.getScheme(), baseURI.getUserInfo(), baseURI.getHost(), baseURI.getPort(), baseURI.getPath(), baseQuery, baseURI.getFragment()).toURL();
        } catch (MalformedURLException ex) {
            Log.e(TAG, "The URL could not be created from " + baseURL.toString(), ex);
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            Log.e(TAG, "Error turning " + query + " into URI query.", ex);
            throw new RuntimeException(ex);
        }
    }

    private JsonElement getResultElement(JsonElement element, String dataRoot) {
        String[] identifiers = dataRoot.split("\\.");
        for (String identifier : identifiers) {
            if (identifier.equals("")) {
                return element;
            }
            JsonElement newElement = element.getAsJsonObject().get(identifier);
            if (newElement == null) {
                return element;
            } else {
                element = newElement;
            }
        }
        return element;
    }

    void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public String getDataRoot() {
        return dataRoot;
    }

    protected RequestBuilder<T> getRequestBuilder() {
        return requestBuilder;
    }

    private boolean retryAuth(AuthenticationModule authModule) {
        return authModule != null && authModule.isLoggedIn() && authModule.retryLogin();
    }
}
