package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnviarEmailRequestDTO {

    @JsonbProperty("mensagem")
    private String mensagem;

    // Opcional -- vira Reply-To do email, nunca From (ver EnvioEmailService). Não é
    // persistido em lugar nenhum, por decisão explícita do usuário: não guardar endereço de
    // email de usuário final, só dos missionários.
    @JsonbProperty("emailRemetente")
    private String emailRemetente;

}
