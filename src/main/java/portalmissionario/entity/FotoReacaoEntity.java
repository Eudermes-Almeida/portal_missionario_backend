package portalmissionario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

// Copia exata do desenho de ExperienciaReacaoEntity (mesmas rotinas de "Manifestar"), so
// trocando experienciaId por fotoId -- ver 012_create_table_foto_reacao_e_comentario.sql.
@Entity
@Table(name = "foto_reacao")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoReacaoEntity {

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

    @Column(name = "tipo_reacao")
    private String tipoReacao;

    @Column(name = "reagido_em")
    private OffsetDateTime reagidoEm;

}
