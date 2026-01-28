package com.ssafy.meari.domain.webrtc.config;

import io.openvidu.java.client.OpenVidu;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class OpenViduConfig {

    @Value("${openvidu.url}")
    private String openViduUrl;

    @Value("${openvidu.secret}")

    private String openViduSecret;

    /**
     * OpenVidu dev 서버의 자체 서명 인증서를 허용하기 위해 SSL 검증 비활성화
     * (개발 환경 전용, 운영에서는 제거할 것)
     */
    @PostConstruct
    public void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            log.info("SSL 검증 비활성화 완료 (개발 환경 전용)");
        } catch (Exception e) {
            log.error("SSL 검증 비활성화 실패: {}", e.getMessage());
        }
    }

    @Bean
    public OpenVidu openVidu() {
        return new OpenVidu(openViduUrl, openViduSecret);
    }
}
