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

// Sem @ManyToOne pra FK -- mesmo estilo "flat" ja usado em MensagemEntity. Diferente de
// MensagemEntity, aqui nao ha remetente/destinatario polimorfico: o autor e sempre o
// missionario dono do perfil (ver 010_create_table_experiencias.sql), entao so existe
// missionarioId.
@Entity
@Table(name = "experiencias")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExperienciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "missionario_id")
    private Long missionarioId;

    private String experiencia;

    private LocalDate dia;

    private LocalTime hora;

}
