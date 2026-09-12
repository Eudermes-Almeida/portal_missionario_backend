package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoComentarioDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("fotoId")
    private Long fotoId;

    @JsonbProperty("autorTipo")
    private String autorTipo;

    @JsonbProperty("autorNome")
    private String autorNome;

    @JsonbProperty("autorUnidade")
    private String autorUnidade;

    @JsonbProperty("comentario")
    private String comentario;

    @JsonbProperty("dia")
    private LocalDate dia;

    @JsonbProperty("hora")
    private LocalTime hora;

}
