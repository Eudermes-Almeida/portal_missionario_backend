package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// "caminhoStorage" fica de fora de proposito -- e um detalhe interno de implementacao (caminho
// dentro do bucket do Supabase, usado so pelo backend pra uma futura exclusao), o front so
// precisa da "url" publica pra exibir a foto. "reacoes"/"minhaReacao" seguem o mesmo padrao de
// MensagemDTO/ExperienciaDTO -- carregados em lote (sem N+1) toda vez que a galeria e listada.
// Comentarios em si NAO vem aqui -- sao carregados sob demanda (GET /fotos/{id}/comentario) so
// quando o usuario abre a foto no lightbox, ja que podem ser mais pesados que um resumo de
// reacao e a grade nao precisa do conteudo deles. "temComentario" e so um booleano leve (tem
// pelo menos 1 ou nao) pra desenhar o "sinal" na miniatura da grade -- pedido explicito do
// usuario, pra distinguir visualmente foto com comentario/reacao de foto sem.
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

    @JsonbProperty("reacoes")
    private List<ReacaoResumoDTO> reacoes;

    @JsonbProperty("minhaReacao")
    private String minhaReacao;

    @JsonbProperty("temComentario")
    private Boolean temComentario;

}
