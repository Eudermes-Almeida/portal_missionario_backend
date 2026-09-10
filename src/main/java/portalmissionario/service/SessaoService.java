package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.entity.SessaoEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SessaoService {

    // Mesma decisao ja validada em producao no projeto irmao RAIO_X_UNIDADE: expira 60 min
    // apos o login, contado a partir de "criado_em" (janela fixa, nao renovada a cada
    // requisicao).
    private static final long DURACAO_SESSAO_MINUTOS = 60;

    @Inject
    EntityManager entityManager;

    // UUID aleatorio (122 bits de entropia) como token opaco -- nao e JWT, nao carrega claim
    // nenhum, so identifica a linha em "sessoes".
    @Transactional
    public String criarSessao(Long membroId) {
        String token = UUID.randomUUID().toString();

        SessaoEntity sessao = SessaoEntity.builder()
                .membroId(membroId)
                .token(token)
                .criadoEm(LocalDateTime.now())
                .build();

        entityManager.persist(sessao);

        return token;
    }

    // Usado pelo filtro central de autorizacao e por qualquer endpoint que precise resolver
    // "quem e o dono deste token". Sessao expirada e tratada exatamente como token
    // inexistente (Optional.empty()) -- e a linha e apagada aqui mesmo, nao fica como lixo
    // esperando um logout que nunca vai vir.
    @Transactional
    public Optional<MatrizAcessoEntity> buscaMembroPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        List<SessaoEntity> sessoes = entityManager.createQuery(
                        "SELECT s FROM SessaoEntity s WHERE s.token = :token",
                        SessaoEntity.class)
                .setParameter("token", token.trim())
                .getResultList();

        if (sessoes.isEmpty()) {
            return Optional.empty();
        }

        SessaoEntity sessao = sessoes.get(0);
        LocalDateTime expiraEm = sessao.getCriadoEm().plusMinutes(DURACAO_SESSAO_MINUTOS);

        if (LocalDateTime.now().isAfter(expiraEm)) {
            entityManager.remove(sessao);
            return Optional.empty();
        }

        return Optional.ofNullable(entityManager.find(MatrizAcessoEntity.class, sessao.getMembroId()));
    }

    // Logout e idempotente -- apagar um token que ja nao existe (ou nunca existiu) nao e erro.
    @Transactional
    public void encerrarSessao(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        entityManager.createQuery("DELETE FROM SessaoEntity s WHERE s.token = :token")
                .setParameter("token", token.trim())
                .executeUpdate();
    }

}
