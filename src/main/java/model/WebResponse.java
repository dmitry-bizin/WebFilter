package model;

import classifiers.bayes.BayesClassifier;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.TEXT_HTML;
import static org.apache.lucene.util.IOUtils.UTF_8;
import static org.eclipse.jetty.http.HttpHeaderValue.IDENTITY;
import static org.json.HTTP.CRLF;
import static util.FileUtil.getHostInBlacklistPage;

public class WebResponse extends Web {

    private static final Logger LOGGER = Logger.getLogger(WebResponse.class);
    private int statusCode;
    private String reasonPhrase;
    private String bodyEncoding;
    private String mimeType;
    private byte[] body;
    private Map<String, String> headers;
    private String version;

    public WebResponse() {
        headers = new HashMap<>();
    }

    public static WebResponse hostInBlacklistResponse() {
        WebResponse webResponse = new WebResponse();
        webResponse.setStatusCode(SC_OK);
        webResponse.setReasonPhrase("");
        webResponse.setVersion("HTTP/1.1");
        webResponse.setHeaders(new HashMap<>());
        webResponse.getHeaders().put(TRANSFER_ENCODING, IDENTITY.toString());
        byte[] body = getHostInBlacklistPage();
        webResponse.setBody(body);
        return webResponse;
    }

    public boolean isHtml() {
        return TEXT_HTML.getMimeType().equals(mimeType);
    }

    public Map<String, Double> classifyContent() throws UnsupportedEncodingException {
        String bodyString = new String(body, getBodyEncoding());
        Pattern pattern = Pattern.compile("[а-яА-Я]+");
        Matcher matcher = pattern.matcher(Jsoup.parse(bodyString).text());
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return BayesClassifier.classify(words);
    }

    public void createProbabilitiesPage(Map<String, Double> categoryProbabilityMap) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(buffer)) {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
            configuration.setClassForTemplateLoading(this.getClass(), "/web/html");
            Template template = configuration.getTemplate("probabilities.ftl", Locale.UK);
            Map<String, Object> rows = new HashMap<>();
            rows.put("probabilities", categoryProbabilityMap.entrySet().stream()
                    .map(entry -> new Probability(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
            template.process(rows, writer);
            writer.flush();
            setStatusCode(SC_OK);
            setReasonPhrase("");
            setVersion("HTTP/1.1");
            setHeaders(new HashMap<>());
            getHeaders().put(TRANSFER_ENCODING, IDENTITY.toString());
            setBody(buffer.toByteArray());
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }

    public String getBodyEncoding() {
        return bodyEncoding != null ? bodyEncoding : UTF_8;
    }

    public void setBodyEncoding(String bodyEncoding) {
        this.bodyEncoding = bodyEncoding;
    }

    @SuppressWarnings("Duplicates")
    public byte[] getAllResponseInBytes() {
        byte[] result = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            AtomicReference<StringBuilder> stringBuilder = new AtomicReference<>(new StringBuilder());
            stringBuilder.get().append(getVersion()).append(" ").append(getStatusCode()).append(" ").append(getReasonPhrase()).append(CRLF);
            for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
                stringBuilder.get().append(entry.getKey()).append(": ").append(entry.getValue()).append(CRLF);
            }
            stringBuilder.get().append(CRLF);
            byteArrayOutputStream.write(stringBuilder.toString().getBytes());
            if (body != null) {
                byteArrayOutputStream.write(body);
            }
            byteArrayOutputStream.flush();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return result;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
