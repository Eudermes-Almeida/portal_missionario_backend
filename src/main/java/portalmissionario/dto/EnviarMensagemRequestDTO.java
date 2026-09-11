package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnviarMensagemRequestDTO {

    // remetente NÃO entra aqui -- é sempre o dono do token da sessão (ver MensagemResource),
    // pra ninguém poder mandar mensagem se passando por outro membro.
    @JsonbProperty("mensagemPaiId")
    private Long mensagemPaiId;

    @JsonbProperty("destinatarioTipo")
    private String destinatarioTipo;

    @JsonbProperty("destinatarioId")
    private Long destinatarioId;

    @JsonbProperty("mensagem")
    private String mensagem;

}
