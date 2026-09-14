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

// Copia de FotoComentarioEntity, so trocando fotoId por experienciaId -- autor_nome/
// autor_unidade ficam CONGELADOS no momento do comentario (ver
// 013_create_table_experiencia_comentario.sql).
@Entity
@Table(name = "experiencia_comentario")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExperienciaComentarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiencia_id")
    private Long experienciaId;

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
