package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.EscreverExperienciaRequestDTO;
import portalmissionario.dto.ExperienciaDTO;
import portalmissionario.dto.ReacaoResumoDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.ExperienciaEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Relatos de experiencia missionaria escritos pelo PROPRIO missionario sobre si mesmo,
// publicos no perfil dele -- mesma tratativa de MensagemService (escrever + listar), mas sem
// remetente/destinatario polimorfico: o autor e sempre o missionario-alvo (ver
// 010_create_table_experiencias.sql).
@ApplicationScoped
public class ExperienciaService {

    private static final int EXPERIENCIA_MAX_CARACTERES = 10000;

    // TODO-DES (temporário, pedido do usuário 2026-09-12): restrição "só o próprio
    // missionário escreve" desativada pra facilitar teste em DES, com qualquer membro logado
    // liberado a escrever experiência em qualquer perfil. REATIVAR (voltar pra `true`) antes
    // de sair do ambiente de DES -- ver o mesmo marcador/flag em
    // DadosMissionariosService.RESTRICAO_PROPRIO_MISSIONARIO_ATIVA (controla o botão no
    // front) e trocar as duas juntas.
    private static final boolean RESTRICAO_PROPRIO_MISSIONARIO_ATIVA = false;

    @Inject
    EntityManager entityManager;

    @Inject
    ExperienciaReacaoService experienciaReacaoService;

    @Inject
    ExperienciaComentarioService experienciaComentarioService;

    // membroAutorId e o dono do token da sessao (resolvido no ExperienciaResource via
    // SessaoService, nunca vem no corpo) -- so pode escrever quem, logado, e a MESMA pessoa do
    // missionario-alvo. Esse vinculo e feito comparando "registromembro" das duas tabelas (o
    // mesmo fator secreto de identificacao usado no Primeiro Acesso -- ver comentario em
    // 003_create_table_dadosmissionarios.sql), nao o id, ja que matriz_acesso e
    // dadosmissionarios sao tabelas independentes.
    @Transactional
    public ExperienciaDTO escreverExperiencia(Long missionarioId, Long membroAutorId, EscreverExperienciaRequestDTO request) {
        if (request == null || request.getExperiencia() == null || request.getExperiencia().trim().isEmpty()) {
            throw new IllegalArgumentException("A experiência é obrigatória.");
        }
        if (request.getExperiencia().length() > EXPERIENCIA_MAX_CARACTERES) {
            throw new IllegalArgumentException("A experiência excede o limite de " + EXPERIENCIA_MAX_CARACTERES + " caracteres.");
        }

        DadosMissionariosEntity missionario = entityManager.find(DadosMissionariosEntity.class, missionarioId);
        if (missionario == null) {
            throw new NotFoundException("Missionário não encontrado: " + missionarioId);
        }

        if (RESTRICAO_PROPRIO_MISSIONARIO_ATIVA) {
            MatrizAcessoEntity membro = entityManager.find(MatrizAcessoEntity.class, membroAutorId);
            if (membro == null
                    || membro.getRegistromembro() == null
                    || missionario.getRegistromembro() == null
                    || !membro.getRegistromembro().equals(missionario.getRegistromembro())) {
                throw new ForbiddenException("Somente o próprio missionário pode escrever uma experiência no seu perfil.");
            }
        }

        ExperienciaEntity entity = ExperienciaEntity.builder()
                .missionarioId(missionarioId)
                .experiencia(request.getExperiencia().trim())
                .dia(LocalDate.now())
                .hora(LocalTime.now())
                .build();
        entityManager.persist(entity);

        return mapToDTO(entity, Collections.emptyList(), null, 0L);
    }

    // Mural público de experiências do missionário, mais recente primeiro. Qualquer membro
    // logado pode ver. membroIdAtual preenche "minhaReacao" de cada experiência (null se não
    // informado).
    @Transactional
    public List<ExperienciaDTO> buscaExperienciasPorMissionario(Long missionarioId, Long membroIdAtual) {
        List<ExperienciaEntity> entidades = entityManager.createQuery(
                        "SELECT e FROM ExperienciaEntity e " +
                                "WHERE e.missionarioId = :id " +
                                "ORDER BY e.dia DESC, e.hora DESC",
                        ExperienciaEntity.class)
                .setParameter("id", missionarioId)
                .getResultList();

        List<Long> experienciaIds = entidades.stream().map(ExperienciaEntity::getId).toList();
        Map<Long, List<ReacaoResumoDTO>> resumos = experienciaReacaoService.resumoPorExperiencias(experienciaIds);
        Map<Long, String> minhasReacoes = experienciaReacaoService.minhasReacoes(experienciaIds, membroIdAtual);
        Map<Long, Long> quantidadesComentarios = experienciaComentarioService.quantidadePorExperiencias(experienciaIds);

        return entidades.stream()
                .map(entity -> mapToDTO(
                        entity,
                        resumos.getOrDefault(entity.getId(), Collections.emptyList()),
                        minhasReacoes.get(entity.getId()),
                        quantidadesComentarios.getOrDefault(entity.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private ExperienciaDTO mapToDTO(ExperienciaEntity entity, List<ReacaoResumoDTO> reacoes, String minhaReacao, Long quantidadeComentarios) {
        return ExperienciaDTO.builder()
                .id(entity.getId())
                .missionarioId(entity.getMissionarioId())
                .experiencia(entity.getExperiencia())
                .dia(entity.getDia())
                .hora(entity.getHora())
                .reacoes(reacoes)
                .minhaReacao(minhaReacao)
                .quantidadeComentarios(quantidadeComentarios)
                .build();
    }

}
