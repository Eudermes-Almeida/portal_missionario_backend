package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// remetenteId/destinatarioId aqui sao um unico campo (nao dois, como na entidade) --
// o polimorfismo remetente_membro_id/remetente_missionario_id e um detalhe de persistencia;
// pra quem consome a API basta saber o tipo ('MEMBRO' ou 'MISSIONARIO') e o id dentro daquela
// tabela.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({
        "id",
        "mensagemPaiId",
        "remetenteTipo",
        "remetenteId",
        "remetenteNome",
        "remetenteUnidade",
        "destinatarioTipo",
        "destinatarioId",
        "destinatarioNome",
        "destinatarioUnidade",
        "mensagem",
        "dia",
        "hora",
        "lida",
        "reacoes",
        "minhaReacao"
})
public class MensagemDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("mensagemPaiId")
    private Long mensagemPaiId;

    @JsonbProperty("remetenteTipo")
    private String remetenteTipo;

    @JsonbProperty("remetenteId")
    private Long remetenteId;

    @JsonbProperty("remetenteNome")
    private String remetenteNome;

    @JsonbProperty("remetenteUnidade")
    private String remetenteUnidade;

    @JsonbProperty("destinatarioTipo")
    private String destinatarioTipo;

    @JsonbProperty("destinatarioId")
    private Long destinatarioId;

    @JsonbProperty("destinatarioNome")
    private String destinatarioNome;

    @JsonbProperty("destinatarioUnidade")
    private String destinatarioUnidade;

    @JsonbProperty("mensagem")
    private String mensagem;

    @JsonbProperty("dia")
    private LocalDate dia;

    @JsonbProperty("hora")
    private LocalTime hora;

    @JsonbProperty("lida")
    private Boolean lida;

    // Resumo de reacoes (tipo + quantidade), so os tipos com pelo menos 1 reacao. Preenchido
    // pelo MensagemReacaoService -- fica de fora quando o caller nao pediu resumo (ver
    // MensagemService).
    @JsonbProperty("reacoes")
    private List<ReacaoResumoDTO> reacoes;

    // Tipo de reacao do membro dono do token da requisicao atual, ou null se ele nao reagiu
    // (ou se nao ha token, ex.: chamada anonima). Nunca reflete reacao de missionario, so de
    // MEMBRO -- unico ator que hoje consegue reagir (missionario nao tem login).
    @JsonbProperty("minhaReacao")
    private String minhaReacao;

}
