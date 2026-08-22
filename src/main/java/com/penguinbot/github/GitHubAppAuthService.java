package com.penguinbot.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penguinbot.config.AppConfig;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubAppAuthService {
    private static final Logger log = LoggerFactory.getLogger(GitHubAppAuthService.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<Long, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private PrivateKey privateKey;

    public GitHubAppAuthService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        loadPrivateKey();
    }

    private void loadPrivateKey() {
        try (PEMParser pemParser = new PEMParser(new FileReader(appConfig.getGithub().getPrivateKeyPath()))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider());
            
            if (object instanceof PEMKeyPair pemKeyPair) {
                this.privateKey = converter.getKeyPair(pemKeyPair).getPrivate();
            } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                this.privateKey = converter.getPrivateKey(privateKeyInfo);
            } else if (object instanceof KeyPair keyPair) {
                this.privateKey = keyPair.getPrivate();
            } else {
                throw new IllegalArgumentException("Unexpected private key format: " + (object != null ? object.getClass().getName() : "null"));
            }
            log.info("Successfully loaded GitHub App private key");
        } catch (Exception e) {
            log.error("Failed to load GitHub App private key from {}", appConfig.getGithub().getPrivateKeyPath(), e);
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    private String generateJwt() {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + 600000; // 10 minutes

        return Jwts.builder()
                .issuer(appConfig.getGithub().getAppId())
                .issuedAt(new Date(nowMillis - 60000)) // 60s clock drift
                .expiration(new Date(expMillis))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String getInstallationToken(long installationId) {
        CachedToken cached = tokenCache.get(installationId);
        if (cached != null && cached.expiry().isAfter(Instant.now().plusSeconds(60))) {
            return cached.token();
        }

        try {
            String jwt = generateJwt();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/app/installations/" + installationId + "/access_tokens"))
                    .header("Authorization", "Bearer " + jwt)
                    .header("Accept", "application/vnd.github+json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Failed to get installation token: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String token = node.get("token").asText();
            Instant expiresAt = Instant.parse(node.get("expires_at").asText());

            tokenCache.put(installationId, new CachedToken(token, expiresAt));
            return token;
        } catch (Exception e) {
            log.error("Error getting installation token for id {}", installationId, e);
            throw new RuntimeException("Error getting installation token", e);
        }
    }

    public GitHub getGitHubClient(long installationId) {
        try {
            String token = getInstallationToken(installationId);
            return new GitHubBuilder().withAppInstallationToken(token).build();
        } catch (Exception e) {
            log.error("Failed to create GitHub client for installation {}", installationId, e);
            throw new RuntimeException("Failed to create GitHub client", e);
        }
    }

    private record CachedToken(String token, Instant expiry) {}
}
