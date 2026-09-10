package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;

// Campos sensiveis (registromembro, nascimento, senha_hash) NAO entram aqui de proposito --
// mesmo tratamento ja validado no projeto irmao RAIO_X_UNIDADE (LideresDTO). registromembro+
// nascimento sao os 2 fatores de identificacao do primeiro acesso; expo-los por essa rota
// permitiria a qualquer um "adivinhar" a identidade de um membro. senha_hash nunca deve
// trafegar pela API, mesmo hasheada.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({
        "id",
        "nome",
        "unidade",
        "escopo",
        "chamado",
        "login"
})
public class MatrizAcessoDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("nome")
    private String nome;

    @JsonbProperty("unidade")
    private String unidade;

    @JsonbProperty("escopo")
    private String escopo;

    @JsonbProperty("chamado")
    private String chamado;

    @JsonbProperty("login")
    private String login;

}
