package com.odontoflow.service;

import com.odontoflow.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SupabaseStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseKey;

    @Value("${supabase.bucket:odontoflow}")
    private String bucket;

    private RestClient client;

    @PostConstruct
    void init() {
        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            log.warn("Supabase Storage não configurado. Uploads vão falhar até SUPABASE_URL e SUPABASE_KEY serem definidos.");
            return;
        }
        client = RestClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("Authorization", "Bearer " + supabaseKey)
                .build();
        log.info("Supabase Storage configurado (bucket={})", bucket);
    }

    public String upload(byte[] bytes, String path, String contentType) {
        ensureConfigured();
        try {
            client.post()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new BusinessException("Falha ao enviar arquivo ao storage: " + body);
                    })
                    .toBodilessEntity();
            return publicUrl(path);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro no upload Supabase path={}", path, e);
            throw new BusinessException("Falha ao enviar arquivo ao storage.");
        }
    }

    public void delete(String path) {
        if (path == null || path.isBlank()) return;
        ensureConfigured();
        try {
            client.delete()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Falha ao remover arquivo path={} status={}", path, res.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao deletar arquivo Supabase path={} — registro será removido mesmo assim", path, e);
        }
    }

    public String publicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private void ensureConfigured() {
        if (client == null) {
            throw new BusinessException("Storage não configurado. Defina SUPABASE_URL e SUPABASE_KEY.");
        }
    }
}
