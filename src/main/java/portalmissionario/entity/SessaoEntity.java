package portalmissionario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

// Sem @ManyToOne para MatrizAcessoEntity de proposito -- mesmo estilo "flat" ja usado no
// projeto irmao RAIO_X_UNIDADE. membro_id e uma FK so a nivel de banco (ver
// 002_create_table_sessoes.sql), lida/gravada como Long puro.
@Entity
@Table(name = "sessoes")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membro_id")
    private Long membroId;

    private String token;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

}
