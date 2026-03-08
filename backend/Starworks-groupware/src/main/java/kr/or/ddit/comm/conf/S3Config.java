package kr.or.ddit.comm.conf;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    String region;

    @Value("${cloud.aws.endpoint:}")
    String endpoint;

    @Value("${cloud.aws.path-style-access:false}")
    boolean pathStyleAccess;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(StringUtils.defaultIfBlank(region, "ap-northeast-2")))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );

        if (StringUtils.isNotBlank(endpoint)) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        if (pathStyleAccess) {
            builder.forcePathStyle(true);
        }

        return builder.build();
    }
}
