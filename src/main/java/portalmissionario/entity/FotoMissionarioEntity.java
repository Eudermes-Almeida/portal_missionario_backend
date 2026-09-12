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

// Sem @ManyToOne pra FK -- mesmo estilo "flat" ja usado em ExperienciaEntity. O autor de uma
// foto e sempre o proprio missionario dono da galeria (ver 011_create_table_fotos_missionario.sql),
// entao so existe missionarioId, sem remetente/destinatario.
@Entity
@Table(name = "fotos_missionario")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FotoMissionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "missionario_id")
    private Long missionarioId;

    private String url;

    @Column(name = "caminho_storage")
    private String caminhoStorage;

    private LocalDate dia;

    private LocalTime hora;

}
