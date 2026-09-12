package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.ReacaoAutorDTO;
import portalmissionario.dto.ReacaoResumoDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.FotoMissionarioEntity;
import portalmissionario.entity.FotoReacaoEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Copia exata de ExperienciaReacaoService (mesmas rotinas de "Manifestar"), so trocando
// experiencia/ExperienciaEntity por foto/FotoMissionarioEntity.
@ApplicationScoped
public class FotoReacaoService {

    private static final String TIPO_MEMBRO = "MEMBRO";

    private static final Set<String> TIPOS_VALIDOS = Set.of("CURTIDA", "DESLIKE", "CORACAO", "SURPRESA", "TRISTEZA");

    @Inject
    EntityManager entityManager;

    @Transactional
    public ResultadoReacao reagir(Long fotoId, Long autorMembroId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        FotoMissionarioEntity foto = entityManager.find(FotoMissionarioEntity.class, fotoId);
        if (foto == null) {
            throw new NotFoundException("Foto não encontrada: " + fotoId);
        }

        FotoReacaoEntity existente = buscaReacaoDoMembro(fotoId, autorMembroId);
        String minhaReacaoFinal;

        if (existente == null) {
            FotoReacaoEntity nova = FotoReacaoEntity.builder()
                    .fotoId(fotoId)
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

        return new ResultadoReacao(resumoPorFoto(fotoId), minhaReacaoFinal);
    }

    @Transactional
    public List<ReacaoResumoDTO> resumoPorFoto(Long fotoId) {
        return entityManager.createQuery(
                        "SELECT r.tipoReacao, COUNT(r) FROM FotoReacaoEntity r " +
                                "WHERE r.fotoId = :fotoId GROUP BY r.tipoReacao",
                        Object[].class)
                .setParameter("fotoId", fotoId)
                .getResultList()
                .stream()
                .map(linha -> ReacaoResumoDTO.builder().tipoReacao((String) linha[0]).quantidade((Long) linha[1]).build())
                .toList();
    }

    @Transactional
    public Map<Long, List<ReacaoResumoDTO>> resumoPorFotos(List<Long> fotoIds) {
        Map<Long, List<ReacaoResumoDTO>> porFoto = new HashMap<>();
        if (fotoIds.isEmpty()) {
            return porFoto;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.fotoId, r.tipoReacao, COUNT(r) FROM FotoReacaoEntity r " +
                                "WHERE r.fotoId IN :ids GROUP BY r.fotoId, r.tipoReacao",
                        Object[].class)
                .setParameter("ids", fotoIds)
                .getResultList();

        for (Object[] linha : linhas) {
            Long fotoId = (Long) linha[0];
            ReacaoResumoDTO item = ReacaoResumoDTO.builder().tipoReacao((String) linha[1]).quantidade((Long) linha[2]).build();
            porFoto.computeIfAbsent(fotoId, k -> new java.util.ArrayList<>()).add(item);
        }

        return porFoto;
    }

    @Transactional
    public Map<Long, String> minhasReacoes(List<Long> fotoIds, Long autorMembroId) {
        Map<Long, String> porFoto = new HashMap<>();
        if (fotoIds.isEmpty() || autorMembroId == null) {
            return porFoto;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.fotoId, r.tipoReacao FROM FotoReacaoEntity r " +
                                "WHERE r.fotoId IN :ids AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        Object[].class)
                .setParameter("ids", fotoIds)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();

        for (Object[] linha : linhas) {
            porFoto.put((Long) linha[0], (String) linha[1]);
        }

        return porFoto;
    }

    @Transactional
    public List<ReacaoAutorDTO> listaAutores(Long fotoId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        List<FotoReacaoEntity> reacoes = entityManager.createQuery(
                        "SELECT r FROM FotoReacaoEntity r " +
                                "WHERE r.fotoId = :fotoId AND r.tipoReacao = :tipo " +
                                "ORDER BY r.reagidoEm DESC",
                        FotoReacaoEntity.class)
                .setParameter("fotoId", fotoId)
                .setParameter("tipo", tipoReacao)
                .getResultList();

        return reacoes.stream().map(this::mapToAutorDTO).toList();
    }

    private ReacaoAutorDTO mapToAutorDTO(FotoReacaoEntity r) {
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

    private FotoReacaoEntity buscaReacaoDoMembro(Long fotoId, Long autorMembroId) {
        List<FotoReacaoEntity> resultado = entityManager.createQuery(
                        "SELECT r FROM FotoReacaoEntity r " +
                                "WHERE r.fotoId = :fotoId AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        FotoReacaoEntity.class)
                .setParameter("fotoId", fotoId)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public record ResultadoReacao(List<ReacaoResumoDTO> resumo, String minhaReacao) {
    }

}
