package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

// Um item do resumo de reacoes de uma mensagem: quantas vezes cada tipo foi escolhido.
// So aparecem aqui tipos com pelo menos 1 reacao (agrupado por GROUP BY no service).
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReacaoResumoDTO {

    @JsonbProperty("tipoReacao")
    private String tipoReacao;

    @JsonbProperty("quantidade")
    private Long quantidade;

}
