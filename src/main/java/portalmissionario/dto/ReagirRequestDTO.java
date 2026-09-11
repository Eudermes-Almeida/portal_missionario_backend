package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReagirRequestDTO {

    // autor NÃO entra aqui -- é sempre o membro dono do token da sessão (ver
    // MensagemResource/MensagemReacaoService), mesmo padrão de EnviarMensagemRequestDTO.
    @JsonbProperty("tipoReacao")
    private String tipoReacao;

}
