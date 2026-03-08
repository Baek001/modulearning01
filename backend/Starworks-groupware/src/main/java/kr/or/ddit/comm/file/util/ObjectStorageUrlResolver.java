package kr.or.ddit.comm.file.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ObjectStorageUrlResolver {

    private final String bucketName;
    private final String endpoint;
    private final String publicBaseUrl;
    private final String region;
    private final boolean pathStyleAccess;

    public ObjectStorageUrlResolver(
        @Value("${cloud.aws.s3.bucket:}") String bucketName,
        @Value("${cloud.aws.endpoint:}") String endpoint,
        @Value("${cloud.aws.public-base-url:}") String publicBaseUrl,
        @Value("${cloud.aws.region.static:ap-northeast-2}") String region,
        @Value("${cloud.aws.path-style-access:false}") boolean pathStyleAccess
    ) {
        this.bucketName = StringUtils.trimToEmpty(bucketName);
        this.endpoint = StringUtils.trimToEmpty(endpoint);
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
        this.region = StringUtils.defaultIfBlank(region, "ap-northeast-2");
        this.pathStyleAccess = pathStyleAccess;
    }

    public String buildObjectKey(String folder, String saveName) {
        return joinKeySegments(folder, saveName);
    }

    public String buildObjectUrl(String folder, String saveName) {
        return buildObjectUrl(buildObjectKey(folder, saveName));
    }

    public String buildObjectUrl(String objectKey) {
        String normalizedKey = trimSlashes(objectKey);
        if (StringUtils.isBlank(normalizedKey)) {
            return "";
        }

        if (StringUtils.isNotBlank(publicBaseUrl)) {
            return joinUrl(publicBaseUrl, normalizedKey);
        }

        if (StringUtils.isNotBlank(endpoint)) {
            String normalizedEndpoint = normalizeBaseUrl(endpoint);
            if (pathStyleAccess) {
                return joinUrl(normalizedEndpoint, bucketName, normalizedKey);
            }

            URI endpointUri = URI.create(normalizedEndpoint);
            String host = endpointUri.getHost();
            if (StringUtils.isBlank(host)) {
                return joinUrl(normalizedEndpoint, normalizedKey);
            }

            String scheme = StringUtils.defaultIfBlank(endpointUri.getScheme(), "https");
            String authority = host;
            if (endpointUri.getPort() >= 0) {
                authority += ":" + endpointUri.getPort();
            }

            String base = scheme + "://" + bucketName + "." + authority;
            String endpointPath = trimSlashes(endpointUri.getPath());
            if (StringUtils.isNotBlank(endpointPath)) {
                base = joinUrl(base, endpointPath);
            }
            return joinUrl(base, normalizedKey);
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + normalizedKey;
    }

    public String extractObjectKey(String storedUrl) {
        String normalizedUrl = StringUtils.trimToEmpty(storedUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return "";
        }
        if (!startsWithHttp(normalizedUrl)) {
            return trimSlashes(normalizedUrl);
        }

        if (StringUtils.isNotBlank(publicBaseUrl)) {
            String strippedByPublicUrl = stripBaseUrl(normalizedUrl, publicBaseUrl);
            if (StringUtils.isNotBlank(strippedByPublicUrl)) {
                return strippedByPublicUrl;
            }
        }

        URI uri = URI.create(normalizedUrl);
        List<String> segments = new ArrayList<>(Arrays.stream(StringUtils.defaultString(uri.getPath()).split("/"))
            .filter(StringUtils::isNotBlank)
            .toList());
        if (segments.isEmpty()) {
            return "";
        }

        if (StringUtils.isNotBlank(endpoint)) {
            URI endpointUri = URI.create(normalizeBaseUrl(endpoint));
            if (StringUtils.equalsIgnoreCase(uri.getHost(), endpointUri.getHost())) {
                List<String> endpointSegments = Arrays.stream(StringUtils.defaultString(endpointUri.getPath()).split("/"))
                    .filter(StringUtils::isNotBlank)
                    .toList();
                if (!endpointSegments.isEmpty() && segments.size() >= endpointSegments.size()) {
                    List<String> leadingSegments = segments.subList(0, endpointSegments.size());
                    if (leadingSegments.equals(endpointSegments)) {
                        segments = new ArrayList<>(segments.subList(endpointSegments.size(), segments.size()));
                    }
                }
            }
        }

        if (!segments.isEmpty() && StringUtils.equalsIgnoreCase(segments.get(0), bucketName)) {
            segments = new ArrayList<>(segments.subList(1, segments.size()));
        }

        return String.join("/", segments);
    }

    private boolean startsWithHttp(String value) {
        return StringUtils.startsWithIgnoreCase(value, "http://")
            || StringUtils.startsWithIgnoreCase(value, "https://");
    }

    private String stripBaseUrl(String fullUrl, String baseUrl) {
        String normalizedBase = normalizeBaseUrl(baseUrl);
        if (StringUtils.isBlank(normalizedBase)) {
            return null;
        }
        if (StringUtils.equalsIgnoreCase(fullUrl, normalizedBase)) {
            return "";
        }

        if (fullUrl.regionMatches(true, 0, normalizedBase, 0, normalizedBase.length())) {
            String remainder = fullUrl.substring(normalizedBase.length());
            if (StringUtils.isEmpty(remainder) || remainder.startsWith("/")) {
                return trimSlashes(remainder);
            }
        }
        return null;
    }

    private String joinUrl(String base, String... segments) {
        String value = normalizeBaseUrl(base);
        for (String segment : segments) {
            String normalizedSegment = trimSlashes(segment);
            if (StringUtils.isBlank(normalizedSegment)) {
                continue;
            }
            value = value + "/" + normalizedSegment;
        }
        return value;
    }

    private String joinKeySegments(String... segments) {
        return Arrays.stream(segments)
            .map(this::trimSlashes)
            .filter(StringUtils::isNotBlank)
            .reduce((left, right) -> left + "/" + right)
            .orElse("");
    }

    private String normalizeBaseUrl(String value) {
        return StringUtils.removeEnd(StringUtils.trimToEmpty(value), "/");
    }

    private String trimSlashes(String value) {
        return StringUtils.strip(StringUtils.defaultString(value), "/");
    }
}
