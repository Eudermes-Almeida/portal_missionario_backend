package portalmissionario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "dadosmissionarios")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DadosMissionariosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_planilha")
    private String idPlanilha;

    private String nomecompleto;

    private String nomemissao;

    private String sexo;

    private String idade;

    private String status;

    private String unidade;

    private String iniciomissao;

    private String finalmissao;

    private String missao;

    private String aniversario;

    private String registromembro;

    private String linkfoto;

    private String email;

}
