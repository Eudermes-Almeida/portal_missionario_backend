package portalmissionario.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.*;

// Deliberadamente NÃO espelha 1:1 todas as colunas da tabela dadosmissionarios: "id_planilha"
// (numeração interna da planilha, sem uso fora dela) e "registromembro" (mesmo fator secreto de
// identificação do Primeiro Acesso usado em matriz_acesso) ficam de fora da resposta da API,
// mesmo padrão já aplicado no projeto irmão RAIO_X_UNIDADE (ver DadosMissionariosDTO de lá).
// "email" também fica de fora por proposito -- só o backend usa o endereço de verdade pra
// disparar o email (ver DadosMissionariosResource/EnvioEmailService); o front só recebe
// "temEmail" (booleano) pra saber se pode oferecer o botão "Enviar Email" sem nunca expor o
// endereço do missionário via API pública.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonbPropertyOrder({
        "id",
        "unidade",
        "nomecompleto",
        "nomemissao",
        "sexo",
        "idade",
        "status",
        "iniciomissao",
        "finalmissao",
        "missao",
        "aniversario",
        "linkfoto",
        "temEmail"
})
public class DadosMissionariosDTO {

    @JsonbProperty("id")
    private Long id;

    @JsonbProperty("unidade")
    private String unidade;

    @JsonbProperty("nomecompleto")
    private String nomecompleto;

    @JsonbProperty("nomemissao")
    private String nomemissao;

    @JsonbProperty("sexo")
    private String sexo;

    @JsonbProperty("idade")
    private String idade;

    @JsonbProperty("status")
    private String status;

    @JsonbProperty("iniciomissao")
    private String iniciomissao;

    @JsonbProperty("finalmissao")
    private String finalmissao;

    @JsonbProperty("missao")
    private String missao;

    @JsonbProperty("aniversario")
    private String aniversario;

    @JsonbProperty("linkfoto")
    private String linkfoto;

    @JsonbProperty("temEmail")
    private Boolean temEmail;

}
