package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// "caminhoStorage" fica de fora de proposito -- e um detalhe interno de implementacao (caminho
// dentro do bucket do Supabase, usado so pelo backend pra uma futura exclusao), o front so
// precisa da "url" publica pra exibir a foto.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoMissionarioDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("missionarioId")
    private Long missionarioId;

    @JsonbProperty("url")
    private String url;

    @JsonbProperty("dia")
    private LocalDate dia;

    @JsonbProperty("hora")
    private LocalTime hora;

}
