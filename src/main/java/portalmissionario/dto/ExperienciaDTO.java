package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExperienciaDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("missionarioId")
    private Long missionarioId;

    @JsonbProperty("experiencia")
    private String experiencia;

    @JsonbProperty("dia")
    private LocalDate dia;

    @JsonbProperty("hora")
    private LocalTime hora;

    @JsonbProperty("reacoes")
    private List<ReacaoResumoDTO> reacoes;

    @JsonbProperty("minhaReacao")
    private String minhaReacao;

    @JsonbProperty("quantidadeComentarios")
    private Long quantidadeComentarios;

}
