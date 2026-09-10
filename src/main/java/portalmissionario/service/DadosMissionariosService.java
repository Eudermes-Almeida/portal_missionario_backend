package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import portalmissionario.dto.DadosMissionariosDTO;
import portalmissionario.entity.DadosMissionariosEntity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class DadosMissionariosService {

    private static final String UNIDADE_MAE = "ESTACA BETIM";

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<DadosMissionariosDTO> buscaTodosDadosMissionarios() throws Exception {
        try {
            List<DadosMissionariosEntity> dadosMissionariosEntities = entityManager.createQuery(
                            "SELECT d FROM DadosMissionariosEntity d ORDER BY d.unidade ASC, d.nomecompleto ASC",
                            DadosMissionariosEntity.class)
                    .getResultList();

            if (dadosMissionariosEntities.isEmpty()) {
                throw new Exception("Nenhum missionário encontrado");
            }

            return dadosMissionariosEntities.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new Exception("Erro ao buscar missionários: " + e.getMessage());
        }
    }

    @Transactional
    public List<DadosMissionariosDTO> buscaDadosMissionariosPorUnidade(String unidade) throws Exception {
        String unidadeTratada = unidade.trim();

        // "Estaca Betim" é a unidade mãe (soma de todas as alas/ramos) e não existe como
        // linha própria na tabela — pedir por ela equivale a trazer todos os registros.
        if (UNIDADE_MAE.equalsIgnoreCase(unidadeTratada)) {
            return buscaTodosDadosMissionarios();
        }

        String jpql = "SELECT d FROM DadosMissionariosEntity d " +
                "WHERE UPPER(d.unidade) = UPPER(:unidade) " +
                "ORDER BY d.nomecompleto ASC";

        List<DadosMissionariosEntity> dadosMissionariosEntities = entityManager.createQuery(jpql, DadosMissionariosEntity.class)
                .setParameter("unidade", unidadeTratada)
                .getResultList();

        return dadosMissionariosEntities.stream()
                .filter(Objects::nonNull)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private DadosMissionariosDTO mapToDTO(DadosMissionariosEntity entity) {
        return DadosMissionariosDTO.builder()
                .id(entity.getId())
                .unidade(entity.getUnidade())
                .nomecompleto(entity.getNomecompleto())
                .nomemissao(entity.getNomemissao())
                .sexo(entity.getSexo())
                .idade(entity.getIdade())
                .status(entity.getStatus())
                .iniciomissao(entity.getIniciomissao())
                .finalmissao(entity.getFinalmissao())
                .missao(entity.getMissao())
                .aniversario(entity.getAniversario())
                .linkfoto(entity.getLinkfoto())
                .build();
    }
}
