package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

// Cliente minimo da API REST do Supabase Storage -- so os 2 endpoints que este projeto precisa
// (subir um objeto novo, montar a URL publica dele). Usa java.net.http.HttpClient puro (ja no
// JDK) em vez de trazer um SDK novo, so pra 2 chamadas simples.
//
// A service_role key AUTORIZA GRAVACAO NO BUCKET INTEIRO, ignorando qualquer RLS -- por isso
// so pode existir aqui (variavel de ambiente, nunca em application.properties com valor
// default, mesmo padrao das credenciais do Brevo) e o upload em si so pode ser disparado pelo
// backend, nunca direto do Angular (decisao explicita do usuario, ver conversa sobre
// estrategia de upload).
@ApplicationScoped
public class SupabaseStorageService {

    @ConfigProperty(name = "supabase.storage.url")
    String supabaseUrl;

    @ConfigProperty(name = "supabase.storage.bucket")
    String bucket;

    // Optional<String> (nao String puro) porque o Quarkus valida @ConfigProperty NO STARTUP
    // -- um campo String obrigatorio sem valor real (nem "", que o converter integrado trata
    // como nulo) derruba o app inteiro assim que a env var nao existe, mesmo antes de alguem
    // tentar subir uma foto. Com Optional, a ausencia vira um Optional.empty() normal, e o
    // erro so aparece (com mensagem clara) quando subir() e chamado de verdade.
    @ConfigProperty(name = "supabase.storage.service-key")
    Optional<String> serviceKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // "caminho" e o caminho dentro do bucket (ex.: galeria/11/uuid.jpg), sem barra inicial.
    // Devolve a URL publica pronta pra persistir/exibir.
    public String subir(String caminho, byte[] bytes, String contentType) {
        String chave = serviceKey.filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalStateException("SUPABASE_SERVICE_KEY não configurada -- configure a variável de ambiente antes de subir fotos."));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + caminho))
                    .header("Authorization", "Bearer " + chave)
                    .header("apikey", chave)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Supabase Storage respondeu " + response.statusCode() + ": " + response.body());
            }

            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + caminho;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao enviar foto pro Supabase Storage: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Envio da foto pro Supabase Storage foi interrompido.", e);
        }
    }

}
