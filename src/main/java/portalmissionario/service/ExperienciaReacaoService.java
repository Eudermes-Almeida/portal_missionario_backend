package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.ReacaoAutorDTO;
import portalmissionario.dto.ReacaoResumoDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.ExperienciaEntity;
import portalmissionario.entity.ExperienciaReacaoEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Copia exata de MensagemReacaoService (mesmas rotinas de "Manifestar"), so trocando
// mensagem/MensagemEntity por experiencia/ExperienciaEntity. So MEMBRO reage hoje (mesma
// justificativa: missionario nao tem login proprio), por isso so existe reagir(membroId, ...).
@ApplicationScoped
public class ExperienciaReacaoService {

    private static final String TIPO_MEMBRO = "MEMBRO";

    private static final Set<String> TIPOS_VALIDOS = Set.of("CURTIDA", "DESLIKE", "CORACAO", "SURPRESA", "TRISTEZA");

    @Inject
    EntityManager entityManager;

    @Transactional
    public ResultadoReacao reagir(Long experienciaId, Long autorMembroId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        ExperienciaEntity experiencia = entityManager.find(ExperienciaEntity.class, experienciaId);
        if (experiencia == null) {
            throw new NotFoundException("Experiência não encontrada: " + experienciaId);
        }

        ExperienciaReacaoEntity existente = buscaReacaoDoMembro(experienciaId, autorMembroId);
        String minhaReacaoFinal;

        if (existente == null) {
            ExperienciaReacaoEntity nova = ExperienciaReacaoEntity.builder()
                    .experienciaId(experienciaId)
                    .autorTipo(TIPO_MEMBRO)
                    .autorMembroId(autorMembroId)
                    .tipoReacao(tipoReacao)
                    .reagidoEm(OffsetDateTime.now())
                    .build();
            entityManager.persist(nova);
            minhaReacaoFinal = tipoReacao;
        } else if (existente.getTipoReacao().equals(tipoReacao)) {
            entityManager.remove(existente);
            minhaReacaoFinal = null;
        } else {
            existente.setTipoReacao(tipoReacao);
            existente.setReagidoEm(OffsetDateTime.now());
            minhaReacaoFinal = tipoReacao;
        }

        return new ResultadoReacao(resumoPorExperiencia(experienciaId), minhaReacaoFinal);
    }

    @Transactional
    public List<ReacaoResumoDTO> resumoPorExperiencia(Long experienciaId) {
        return entityManager.createQuery(
                        "SELECT r.tipoReacao, COUNT(r) FROM ExperienciaReacaoEntity r " +
                                "WHERE r.experienciaId = :experienciaId GROUP BY r.tipoReacao",
                        Object[].class)
                .setParameter("experienciaId", experienciaId)
                .getResultList()
                .stream()
                .map(linha -> ReacaoResumoDTO.builder().tipoReacao((String) linha[0]).quantidade((Long) linha[1]).build())
                .toList();
    }

    @Transactional
    public Map<Long, List<ReacaoResumoDTO>> resumoPorExperiencias(List<Long> experienciaIds) {
        Map<Long, List<ReacaoResumoDTO>> porExperiencia = new HashMap<>();
        if (experienciaIds.isEmpty()) {
            return porExperiencia;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.experienciaId, r.tipoReacao, COUNT(r) FROM ExperienciaReacaoEntity r " +
                                "WHERE r.experienciaId IN :ids GROUP BY r.experienciaId, r.tipoReacao",
                        Object[].class)
                .setParameter("ids", experienciaIds)
                .getResultList();

        for (Object[] linha : linhas) {
            Long experienciaId = (Long) linha[0];
            ReacaoResumoDTO item = ReacaoResumoDTO.builder().tipoReacao((String) linha[1]).quantidade((Long) linha[2]).build();
            porExperiencia.computeIfAbsent(experienciaId, k -> new java.util.ArrayList<>()).add(item);
        }

        return porExperiencia;
    }

    @Transactional
    public Map<Long, String> minhasReacoes(List<Long> experienciaIds, Long autorMembroId) {
        Map<Long, String> porExperiencia = new HashMap<>();
        if (experienciaIds.isEmpty() || autorMembroId == null) {
            return porExperiencia;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.experienciaId, r.tipoReacao FROM ExperienciaReacaoEntity r " +
                                "WHERE r.experienciaId IN :ids AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        Object[].class)
                .setParameter("ids", experienciaIds)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();

        for (Object[] linha : linhas) {
            porExperiencia.put((Long) linha[0], (String) linha[1]);
        }

        return porExperiencia;
    }

    @Transactional
    public List<ReacaoAutorDTO> listaAutores(Long experienciaId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        List<ExperienciaReacaoEntity> reacoes = entityManager.createQuery(
                        "SELECT r FROM ExperienciaReacaoEntity r " +
                                "WHERE r.experienciaId = :experienciaId AND r.tipoReacao = :tipo " +
                                "ORDER BY r.reagidoEm DESC",
                        ExperienciaReacaoEntity.class)
                .setParameter("experienciaId", experienciaId)
                .setParameter("tipo", tipoReacao)
                .getResultList();

        return reacoes.stream().map(this::mapToAutorDTO).toList();
    }

    private ReacaoAutorDTO mapToAutorDTO(ExperienciaReacaoEntity r) {
        String nome;
        String unidade;

        if (TIPO_MEMBRO.equals(r.getAutorTipo())) {
            MatrizAcessoEntity membro = entityManager.find(MatrizAcessoEntity.class, r.getAutorMembroId());
            nome = membro != null ? membro.getNome() : "Membro removido";
            unidade = membro != null ? membro.getUnidade() : null;
        } else {
            DadosMissionariosEntity missionario = entityManager.find(DadosMissionariosEntity.class, r.getAutorMissionarioId());
            nome = missionario != null ? missionario.getNomemissao() : "Missionário removido";
            unidade = missionario != null ? missionario.getUnidade() : null;
        }

        return ReacaoAutorDTO.builder()
                .autorTipo(r.getAutorTipo())
                .autorNome(nome)
                .autorUnidade(unidade)
                .reagidoEm(r.getReagidoEm())
                .build();
    }

    private ExperienciaReacaoEntity buscaReacaoDoMembro(Long experienciaId, Long autorMembroId) {
        List<ExperienciaReacaoEntity> resultado = entityManager.createQuery(
                        "SELECT r FROM ExperienciaReacaoEntity r " +
                                "WHERE r.experienciaId = :experienciaId AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        ExperienciaReacaoEntity.class)
                .setParameter("experienciaId", experienciaId)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public record ResultadoReacao(List<ReacaoResumoDTO> resumo, String minhaReacao) {
    }

}
