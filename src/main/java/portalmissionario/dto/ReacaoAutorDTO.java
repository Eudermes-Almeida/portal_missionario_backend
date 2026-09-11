package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.OffsetDateTime;

// Um item da lista "quem reagiu com este ícone" (ver MensagemReacaoService.listaAutores).
// autorNome/autorUnidade são resolvidos ao vivo (não congelados como remetente/destinatario
// em MensagemEntity) -- reação é mutável (pode trocar/remover), então não faz sentido
// congelar um snapshot histórico como faz a mensagem em si.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReacaoAutorDTO {

    @JsonbProperty("autorTipo")
    private String autorTipo;

    @JsonbProperty("autorNome")
    private String autorNome;

    @JsonbProperty("autorUnidade")
    private String autorUnidade;

    @JsonbProperty("reagidoEm")
    private OffsetDateTime reagidoEm;

}
