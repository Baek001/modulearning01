package kr.or.ddit.comm.file.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObjectStorageUrlResolverTest {

    @Test
    void buildsPublicBaseUrlForR2CustomDomain() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            "https://1234567890abcdef.r2.cloudflarestorage.com",
            "https://files.example.com",
            "auto",
            true
        );

        assertThat(resolver.buildObjectUrl("approval", "doc.pdf"))
            .isEqualTo("https://files.example.com/approval/doc.pdf");
    }

    @Test
    void extractsKeyFromCustomPublicDomain() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            "https://1234567890abcdef.r2.cloudflarestorage.com",
            "https://files.example.com",
            "auto",
            true
        );

        assertThat(resolver.extractObjectKey("https://files.example.com/message/abc123.png"))
            .isEqualTo("message/abc123.png");
    }

    @Test
    void buildsPathStyleEndpointUrlWhenConfigured() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            "https://1234567890abcdef.r2.cloudflarestorage.com",
            "",
            "auto",
            true
        );

        assertThat(resolver.buildObjectUrl("board", "file.txt"))
            .isEqualTo("https://1234567890abcdef.r2.cloudflarestorage.com/starworks-files/board/file.txt");
    }

    @Test
    void extractsKeyFromPathStyleEndpointUrl() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            "https://1234567890abcdef.r2.cloudflarestorage.com",
            "",
            "auto",
            true
        );

        assertThat(
            resolver.extractObjectKey("https://1234567890abcdef.r2.cloudflarestorage.com/starworks-files/board/file.txt")
        ).isEqualTo("board/file.txt");
    }

    @Test
    void fallsBackToAwsStyleUrlWhenNoCustomEndpointExists() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            "",
            "",
            "ap-northeast-2",
            false
        );

        assertThat(resolver.buildObjectUrl("profile", "avatar.png"))
            .isEqualTo("https://starworks-files.s3.ap-northeast-2.amazonaws.com/profile/avatar.png");
    }
}
