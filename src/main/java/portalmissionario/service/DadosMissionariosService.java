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

    // TODO-DES (temporário, pedido do usuário 2026-09-12): restrição "só o próprio
    // missionário escreve experiência" desativada pra facilitar teste em DES -- com a flag em
    // false, "podeEscreverExperiencia" sai `true` pra todo mundo, habilitando o botão no front
    // pra qualquer missionário. REATIVAR (voltar pra `true`) antes de sair do ambiente de DES
    // -- ver o mesmo marcador/flag em ExperienciaService.RESTRICAO_PROPRIO_MISSIONARIO_ATIVA
    // (bloqueia a escrita em si) e trocar as duas juntas.
    private static final boolean RESTRICAO_PROPRIO_MISSIONARIO_ATIVA = false;

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<DadosMissionariosDTO> buscaTodosDadosMissionarios() throws Exception {
        return buscaTodosDadosMissionarios(null);
    }

    // registromembroLogado (do membro dono do token, resolvido no Resource) só serve pra
    // calcular "podeEscreverExperiencia" por linha -- null quando a requisição não tem sessão
    // válida (o campo simplesmente sai false pra todo mundo nesse caso).
    @Transactional
    public List<DadosMissionariosDTO> buscaTodosDadosMissionarios(String registromembroLogado) throws Exception {
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
                    .map(entity -> mapToDTO(entity, registromembroLogado))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new Exception("Erro ao buscar missionários: " + e.getMessage());
        }
    }

    @Transactional
    public List<DadosMissionariosDTO> buscaDadosMissionariosPorUnidade(String unidade, String registromembroLogado) throws Exception {
        String unidadeTratada = unidade.trim();

        // "Estaca Betim" é a unidade mãe (soma de todas as alas/ramos) e não existe como
        // linha própria na tabela — pedir por ela equivale a trazer todos os registros.
        if (UNIDADE_MAE.equalsIgnoreCase(unidadeTratada)) {
            return buscaTodosDadosMissionarios(registromembroLogado);
        }

        String jpql = "SELECT d FROM DadosMissionariosEntity d " +
                "WHERE UPPER(d.unidade) = UPPER(:unidade) " +
                "ORDER BY d.nomecompleto ASC";

        List<DadosMissionariosEntity> dadosMissionariosEntities = entityManager.createQuery(jpql, DadosMissionariosEntity.class)
                .setParameter("unidade", unidadeTratada)
                .getResultList();

        return dadosMissionariosEntities.stream()
                .filter(Objects::nonNull)
                .map(entity -> mapToDTO(entity, registromembroLogado))
                .collect(Collectors.toList());
    }

    private DadosMissionariosDTO mapToDTO(DadosMissionariosEntity entity, String registromembroLogado) {
        boolean podeEscreverExperiencia = !RESTRICAO_PROPRIO_MISSIONARIO_ATIVA
                || (registromembroLogado != null
                        && entity.getRegistromembro() != null
                        && registromembroLogado.equals(entity.getRegistromembro()));

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
                .temEmail(entity.getEmail() != null && !entity.getEmail().isBlank())
                .podeEscreverExperiencia(podeEscreverExperiencia)
                .build();
    }
}
