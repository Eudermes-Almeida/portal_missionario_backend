package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;

// Devolvido so quando a identificacao encontra a pessoa E ela ainda nao tem login/senha
// cadastrados -- usado pela tela de confirmacao ("Voce e Fulano de tal?"). O "id" aqui so e
// revelado depois que quem chamou ja provou conhecer nascimento+registromembro corretos,
// entao nao e um enumeration risk (ver MatrizAcessoDTO, que nunca expoe esses 2 campos).
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({"id", "nome"})
public class IdentificacaoResponseDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("nome")
    private String nome;

}
