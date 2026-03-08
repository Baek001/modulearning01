package kr.or.ddit.comm.file.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObjectStorageUrlResolverTest {

    private static final String SUPABASE_S3_ENDPOINT = "https://project-ref.storage.supabase.co/storage/v1/s3";
    private static final String SUPABASE_PUBLIC_BASE_URL = "https://project-ref.supabase.co/storage/v1/object/public/starworks-files";

    @Test
    void buildsPublicBaseUrlForSupabaseStorage() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            SUPABASE_S3_ENDPOINT,
            SUPABASE_PUBLIC_BASE_URL,
            "auto",
            true
        );

        assertThat(resolver.buildObjectUrl("approval", "doc.pdf"))
            .isEqualTo("https://project-ref.supabase.co/storage/v1/object/public/starworks-files/approval/doc.pdf");
    }

    @Test
    void extractsKeyFromSupabasePublicBaseUrl() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            SUPABASE_S3_ENDPOINT,
            SUPABASE_PUBLIC_BASE_URL,
            "auto",
            true
        );

        assertThat(
            resolver.extractObjectKey(
                "https://project-ref.supabase.co/storage/v1/object/public/starworks-files/message/abc123.png"
            )
        )
            .isEqualTo("message/abc123.png");
    }

    @Test
    void buildsPathStyleEndpointUrlForSupabaseS3() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            SUPABASE_S3_ENDPOINT,
            "",
            "auto",
            true
        );

        assertThat(resolver.buildObjectUrl("board", "file.txt"))
            .isEqualTo("https://project-ref.storage.supabase.co/storage/v1/s3/starworks-files/board/file.txt");
    }

    @Test
    void extractsKeyFromPathStyleEndpointUrl() {
        ObjectStorageUrlResolver resolver = new ObjectStorageUrlResolver(
            "starworks-files",
            SUPABASE_S3_ENDPOINT,
            "",
            "auto",
            true
        );

        assertThat(
            resolver.extractObjectKey(
                "https://project-ref.storage.supabase.co/storage/v1/s3/starworks-files/board/file.txt"
            )
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
