package portalmissionario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

// Sem @ManyToOne pras FKs -- mesmo estilo "flat" de MensagemEntity. autor e polimorfico
// (matriz_acesso OU dadosmissionarios; ver 008_create_table_mensagens.sql), mas na pratica
// hoje so MEMBRO reage (missionario nao tem login) -- a coluna missionario existe pra manter
// o mesmo desenho polimorfico do restante do sistema, nao por uso atual.
@Entity
@Table(name = "mensagem_reacao")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MensagemReacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mensagem_id")
    private Long mensagemId;

    @Column(name = "autor_tipo")
    private String autorTipo;

    @Column(name = "autor_membro_id")
    private Long autorMembroId;

    @Column(name = "autor_missionario_id")
    private Long autorMissionarioId;

    @Column(name = "tipo_reacao")
    private String tipoReacao;

    @Column(name = "reagido_em")
    private OffsetDateTime reagidoEm;

}
