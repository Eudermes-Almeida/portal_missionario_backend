package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EscreverExperienciaRequestDTO {

    // O missionario-alvo vem do path (/experiencias/missionario/{id}), nao daqui -- e o
    // backend valida que quem esta escrevendo (dono do token) e o proprio missionario (ver
    // ExperienciaService.escreverExperiencia).
    @JsonbProperty("experiencia")
    private String experiencia;

}
