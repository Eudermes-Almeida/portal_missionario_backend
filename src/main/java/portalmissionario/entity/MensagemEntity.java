package portalmissionario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// Sem @ManyToOne pras FKs -- mesmo estilo "flat" ja usado em SessaoEntity/DadosMissionariosEntity.
// remetente/destinatario sao polimorficos (matriz_acesso OU dadosmissionarios; ver
// 008_create_table_mensagens.sql), por isso cada lado tem duas colunas de id (uma sempre null)
// mais o _tipo dizendo qual das duas vale -- a integridade dessa regra e garantida pelas
// CHECK constraints no banco, nao repetida aqui via Bean Validation.
@Entity
@Table(name = "mensagens")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MensagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mensagem_pai_id")
    private Long mensagemPaiId;

    @Column(name = "remetente_tipo")
    private String remetenteTipo;

    @Column(name = "remetente_membro_id")
    private Long remetenteMembroId;

    @Column(name = "remetente_missionario_id")
    private Long remetenteMissionarioId;

    @Column(name = "remetente_nome")
    private String remetenteNome;

    @Column(name = "remetente_unidade")
    private String remetenteUnidade;

    @Column(name = "destinatario_tipo")
    private String destinatarioTipo;

    @Column(name = "destinatario_membro_id")
    private Long destinatarioMembroId;

    @Column(name = "destinatario_missionario_id")
    private Long destinatarioMissionarioId;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    @Column(name = "destinatario_unidade")
    private String destinatarioUnidade;

    private String mensagem;

    private LocalDate dia;

    private LocalTime hora;

    private Boolean lida;

}
