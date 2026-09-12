package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EscreverComentarioRequestDTO {

    // autor NÃO entra aqui -- é sempre o membro dono do token da sessão (ver
    // FotoComentarioResource/FotoComentarioService), mesmo padrão de EnviarMensagemRequestDTO.
    @JsonbProperty("comentario")
    private String comentario;

}
