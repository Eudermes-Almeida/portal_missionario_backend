package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnviarMensagemRequestDTO {

    @JsonbProperty("mensagemPaiId")
    private Long mensagemPaiId;

    @JsonbProperty("remetenteTipo")
    private String remetenteTipo;

    @JsonbProperty("remetenteId")
    private Long remetenteId;

    @JsonbProperty("destinatarioTipo")
    private String destinatarioTipo;

    @JsonbProperty("destinatarioId")
    private Long destinatarioId;

    @JsonbProperty("mensagem")
    private String mensagem;

}
