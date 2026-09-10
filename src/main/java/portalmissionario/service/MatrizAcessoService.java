package portalmissionario.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import portalmissionario.dto.MatrizAcessoDTO;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class MatrizAcessoService {

    private static final String UNIDADE_MAE = "ESTACA BETIM";

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<MatrizAcessoDTO> buscaTodosMembros() throws Exception {
        try {
            List<MatrizAcessoEntity> entities = entityManager.createQuery(
                            "SELECT m FROM MatrizAcessoEntity m ORDER BY m.unidade ASC, m.nome ASC",
                            MatrizAcessoEntity.class)
                    .getResultList();

            if (entities.isEmpty()) {
                throw new Exception("Nenhum membro encontrado");
            }

            return entities.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new Exception("Erro ao buscar membros: " + e.getMessage());
        }
    }

    @Transactional
    public List<MatrizAcessoDTO> buscaMembrosPorUnidade(String unidade) throws Exception {
        String unidadeTratada = unidade.trim();

        // "Estaca Betim" e a unidade mae (soma de todas as alas/ramos) -- pedir por ela
        // deve trazer TODOS os membros, mesmo padrao das outras tabelas do projeto irmao.
        if (UNIDADE_MAE.equalsIgnoreCase(unidadeTratada)) {
            return buscaTodosMembros();
        }

        String jpql = "SELECT m FROM MatrizAcessoEntity m " +
                "WHERE UPPER(m.unidade) = UPPER(:unidade) " +
                "ORDER BY m.nome ASC";

        List<MatrizAcessoEntity> entities = entityManager.createQuery(jpql, MatrizAcessoEntity.class)
                .setParameter("unidade", unidadeTratada)
                .getResultList();

        return entities.stream()
                .filter(Objects::nonNull)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // --- Suporte ao fluxo de autenticacao (identificacao/login) ---
    // Metodos abaixo trabalham direto com a Entity (nunca com o DTO "seguro" acima), ja que
    // precisam ler/gravar registromembro e senha_hash -- campos que o DTO deliberadamente
    // nao expoe pela API de leitura.

    @Transactional
    public Optional<MatrizAcessoEntity> buscaPorNascimentoERegistroMembro(LocalDate nascimento, String ultimosQuatroRegistro) {
        List<MatrizAcessoEntity> candidatos = entityManager.createQuery(
                        "SELECT m FROM MatrizAcessoEntity m WHERE m.nascimento = :nascimento",
                        MatrizAcessoEntity.class)
                .setParameter("nascimento", nascimento)
                .getResultList();

        return candidatos.stream()
                .filter(m -> terminaCom(m.getRegistromembro(), ultimosQuatroRegistro))
                .findFirst();
    }

    @Transactional
    public Optional<MatrizAcessoEntity> buscaMembroPorId(Long id) {
        return Optional.ofNullable(entityManager.find(MatrizAcessoEntity.class, id));
    }

    @Transactional
    public Optional<MatrizAcessoEntity> buscaPorLogin(String login) {
        return entityManager.createQuery(
                        "SELECT m FROM MatrizAcessoEntity m WHERE UPPER(m.login) = UPPER(:login)",
                        MatrizAcessoEntity.class)
                .setParameter("login", login.trim())
                .getResultList()
                .stream()
                .findFirst();
    }

    @Transactional
    public boolean loginJaExiste(String login) {
        Long total = entityManager.createQuery(
                        "SELECT COUNT(m) FROM MatrizAcessoEntity m WHERE UPPER(m.login) = UPPER(:login)",
                        Long.class)
                .setParameter("login", login.trim())
                .getSingleResult();
        return total > 0;
    }

    // Unico ponto do projeto que grava senha -- sempre em hash (BCrypt), nunca em texto puro.
    // Nao valida duplicidade de login nem se o registro ja estava cadastrado; isso e
    // responsabilidade do Resource, que decide o status HTTP de cada caso antes de chamar
    // este metodo.
    @Transactional
    public void cadastrarCredenciais(Long id, String login, String senhaPlana) {
        MatrizAcessoEntity entity = entityManager.find(MatrizAcessoEntity.class, id);
        entity.setLogin(login.trim());
        entity.setSenhaHash(BcryptUtil.bcryptHash(senhaPlana));
    }

    private boolean terminaCom(String registromembro, String ultimosQuatro) {
        if (registromembro == null || ultimosQuatro == null) {
            return false;
        }
        String valor = registromembro.trim();
        String sufixo = ultimosQuatro.trim();
        return valor.length() >= sufixo.length()
                && valor.substring(valor.length() - sufixo.length()).equalsIgnoreCase(sufixo);
    }

    private MatrizAcessoDTO mapToDTO(MatrizAcessoEntity entity) {
        return MatrizAcessoDTO.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .unidade(entity.getUnidade())
                .escopo(entity.getEscopo())
                .chamado(entity.getChamado())
                .login(entity.getLogin())
                .build();
    }
}
