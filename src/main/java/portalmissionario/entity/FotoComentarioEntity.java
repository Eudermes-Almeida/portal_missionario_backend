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

// Mais parecida com MensagemEntity do que com FotoReacaoEntity: autor_nome/autor_unidade ficam
// CONGELADOS no momento do comentario (decisao explicita do usuario), ja que um comentario e um
// texto permanente escrito uma vez, nao mutavel/togglable como reacao -- ver
// 012_create_table_foto_reacao_e_comentario.sql.
@Entity
@Table(name = "foto_comentario")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoComentarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "foto_id")
    private Long fotoId;

    @Column(name = "autor_tipo")
    private String autorTipo;

    @Column(name = "autor_membro_id")
    private Long autorMembroId;

    @Column(name = "autor_missionario_id")
    private Long autorMissionarioId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(name = "autor_unidade")
    private String autorUnidade;

    private String comentario;

    private LocalDate dia;

    private LocalTime hora;

}
