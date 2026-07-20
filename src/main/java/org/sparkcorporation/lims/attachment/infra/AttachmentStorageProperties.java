package org.sparkcorporation.lims.attachment.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code lims.attachment.storage.*} (spec §6). */
@ConfigurationProperties("lims.attachment.storage")
public class AttachmentStorageProperties {

    /** {@code filesystem} (default) or {@code s3}. */
    private String backend = "filesystem";

    private Fs fs = new Fs();
    private S3 s3 = new S3();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public Fs getFs() {
        return fs;
    }

    public void setFs(Fs fs) {
        this.fs = fs;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Fs {

        private String root = "var/attachments";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class S3 {

        private String bucket = "lims-attachments";
        private String region = "us-east-1";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyle = true;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyle() {
            return pathStyle;
        }

        public void setPathStyle(boolean pathStyle) {
            this.pathStyle = pathStyle;
        }
    }
}
