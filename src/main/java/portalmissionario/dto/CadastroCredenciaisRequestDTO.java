package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CadastroCredenciaisRequestDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("login")
    private String login;

    @JsonbProperty("senha")
    private String senha;

}
