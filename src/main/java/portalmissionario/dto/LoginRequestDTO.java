package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    @JsonbProperty("login")
    private String login;

    @JsonbProperty("senha")
    private String senha;

}
