package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.ReacaoAutorDTO;
import portalmissionario.dto.ReacaoResumoDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.entity.MensagemEntity;
import portalmissionario.entity.MensagemReacaoEntity;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// "Manifestar" -- reacao publica de um membro a uma mensagem do mural (ver
// 008_create_table_mensagens.sql). So MEMBRO reage hoje (missionario nao tem login), por isso
// so existe reagir(membroId, ...) e nao uma variante MISSIONARIO.
@ApplicationScoped
public class MensagemReacaoService {

    private static final String TIPO_MEMBRO = "MEMBRO";

    // 5 tipos fixos definidos pelo usuario -- mesma lista do CHECK ck_mensagem_reacao_tipo.
    private static final Set<String> TIPOS_VALIDOS = Set.of("CURTIDA", "DESLIKE", "CORACAO", "SURPRESA", "TRISTEZA");

    @Inject
    EntityManager entityManager;

    // Clicar no mesmo icone ja escolhido remove a reacao (toggle off, igual WhatsApp/Facebook
    // -- decisao explicita do usuario); clicar em outro icone troca (UPDATE, nao linha nova,
    // ja que o indice unico parcial garante no maximo 1 reacao por pessoa por mensagem).
    // Devolve o resumo atualizado + a reacao atual do proprio autor (null se removeu).
    @Transactional
    public ResultadoReacao reagir(Long mensagemId, Long autorMembroId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        MensagemEntity mensagem = entityManager.find(MensagemEntity.class, mensagemId);
        if (mensagem == null) {
            throw new NotFoundException("Mensagem não encontrada: " + mensagemId);
        }

        MensagemReacaoEntity existente = buscaReacaoDoMembro(mensagemId, autorMembroId);
        String minhaReacaoFinal;

        if (existente == null) {
            MensagemReacaoEntity nova = MensagemReacaoEntity.builder()
                    .mensagemId(mensagemId)
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

        return new ResultadoReacao(resumoPorMensagem(mensagemId), minhaReacaoFinal);
    }

    @Transactional
    public List<ReacaoResumoDTO> resumoPorMensagem(Long mensagemId) {
        return entityManager.createQuery(
                        "SELECT r.tipoReacao, COUNT(r) FROM MensagemReacaoEntity r " +
                                "WHERE r.mensagemId = :mensagemId GROUP BY r.tipoReacao",
                        Object[].class)
                .setParameter("mensagemId", mensagemId)
                .getResultList()
                .stream()
                .map(linha -> ReacaoResumoDTO.builder().tipoReacao((String) linha[0]).quantidade((Long) linha[1]).build())
                .toList();
    }

    // Versao em lote pra listagem do mural inteiro (buscaMensagensPorMissionario) -- evita
    // N+1 fazendo uma unica query de resumo e uma unica query de "minha reacao" pro conjunto
    // de mensagens, agrupando em memoria por mensagemId.
    @Transactional
    public Map<Long, List<ReacaoResumoDTO>> resumoPorMensagens(List<Long> mensagemIds) {
        Map<Long, List<ReacaoResumoDTO>> porMensagem = new HashMap<>();
        if (mensagemIds.isEmpty()) {
            return porMensagem;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.mensagemId, r.tipoReacao, COUNT(r) FROM MensagemReacaoEntity r " +
                                "WHERE r.mensagemId IN :ids GROUP BY r.mensagemId, r.tipoReacao",
                        Object[].class)
                .setParameter("ids", mensagemIds)
                .getResultList();

        for (Object[] linha : linhas) {
            Long mensagemId = (Long) linha[0];
            ReacaoResumoDTO item = ReacaoResumoDTO.builder().tipoReacao((String) linha[1]).quantidade((Long) linha[2]).build();
            porMensagem.computeIfAbsent(mensagemId, k -> new java.util.ArrayList<>()).add(item);
        }

        return porMensagem;
    }

    @Transactional
    public Map<Long, String> minhasReacoes(List<Long> mensagemIds, Long autorMembroId) {
        Map<Long, String> porMensagem = new HashMap<>();
        if (mensagemIds.isEmpty() || autorMembroId == null) {
            return porMensagem;
        }

        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT r.mensagemId, r.tipoReacao FROM MensagemReacaoEntity r " +
                                "WHERE r.mensagemId IN :ids AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        Object[].class)
                .setParameter("ids", mensagemIds)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();

        for (Object[] linha : linhas) {
            porMensagem.put((Long) linha[0], (String) linha[1]);
        }

        return porMensagem;
    }

    // Lista de "quem reagiu com este ícone" (clique num pill de reação no mural) -- mais
    // recente primeiro. Nome/unidade resolvidos ao vivo (ver comentário em ReacaoAutorDTO).
    @Transactional
    public List<ReacaoAutorDTO> listaAutores(Long mensagemId, String tipoReacao) {
        if (tipoReacao == null || !TIPOS_VALIDOS.contains(tipoReacao)) {
            throw new IllegalArgumentException("Tipo de reação inválido. Use um de: " + TIPOS_VALIDOS);
        }

        List<MensagemReacaoEntity> reacoes = entityManager.createQuery(
                        "SELECT r FROM MensagemReacaoEntity r " +
                                "WHERE r.mensagemId = :mensagemId AND r.tipoReacao = :tipo " +
                                "ORDER BY r.reagidoEm DESC",
                        MensagemReacaoEntity.class)
                .setParameter("mensagemId", mensagemId)
                .setParameter("tipo", tipoReacao)
                .getResultList();

        return reacoes.stream().map(this::mapToAutorDTO).toList();
    }

    private ReacaoAutorDTO mapToAutorDTO(MensagemReacaoEntity r) {
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

    private MensagemReacaoEntity buscaReacaoDoMembro(Long mensagemId, Long autorMembroId) {
        List<MensagemReacaoEntity> resultado = entityManager.createQuery(
                        "SELECT r FROM MensagemReacaoEntity r " +
                                "WHERE r.mensagemId = :mensagemId AND r.autorTipo = :tipo AND r.autorMembroId = :membroId",
                        MensagemReacaoEntity.class)
                .setParameter("mensagemId", mensagemId)
                .setParameter("tipo", TIPO_MEMBRO)
                .setParameter("membroId", autorMembroId)
                .getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public record ResultadoReacao(List<ReacaoResumoDTO> resumo, String minhaReacao) {
    }

}
