package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentificacaoRequestDTO {

    @JsonbProperty("nascimento")
    private LocalDate nascimento;

    @JsonbProperty("ultimosQuatroRegistro")
    private String ultimosQuatroRegistro;

}
