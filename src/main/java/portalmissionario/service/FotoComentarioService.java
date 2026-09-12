package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.EscreverComentarioRequestDTO;
import portalmissionario.dto.FotoComentarioDTO;
import portalmissionario.entity.FotoComentarioEntity;
import portalmissionario.entity.FotoMissionarioEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Comentario numa foto -- mais parecido com MensagemService (autor_nome/autor_unidade
// congelados no momento do envio) do que com FotoReacaoService (mutavel/togglable). Sem
// remetente/destinatario polimorfico: o "destinatario" e sempre a foto (fotoId), e o autor e
// sempre o membro dono do token (nunca vem no corpo). Multiplos comentarios por pessoa na
// mesma foto sao permitidos -- decisao explicita do usuario.
@ApplicationScoped
public class FotoComentarioService {

    private static final String TIPO_MEMBRO = "MEMBRO";
    private static final int COMENTARIO_MAX_CARACTERES = 100;

    @Inject
    EntityManager entityManager;

    @Transactional
    public FotoComentarioDTO escreverComentario(Long fotoId, Long autorMembroId, EscreverComentarioRequestDTO request) {
        if (request == null || request.getComentario() == null || request.getComentario().trim().isEmpty()) {
            throw new IllegalArgumentException("O comentário é obrigatório.");
        }
        if (request.getComentario().length() > COMENTARIO_MAX_CARACTERES) {
            throw new IllegalArgumentException("O comentário excede o limite de " + COMENTARIO_MAX_CARACTERES + " caracteres.");
        }

        FotoMissionarioEntity foto = entityManager.find(FotoMissionarioEntity.class, fotoId);
        if (foto == null) {
            throw new NotFoundException("Foto não encontrada: " + fotoId);
        }

        MatrizAcessoEntity autor = entityManager.find(MatrizAcessoEntity.class, autorMembroId);
        if (autor == null) {
            throw new IllegalArgumentException("Membro não encontrado: " + autorMembroId);
        }

        FotoComentarioEntity entity = FotoComentarioEntity.builder()
                .fotoId(fotoId)
                .autorTipo(TIPO_MEMBRO)
                .autorMembroId(autorMembroId)
                .autorNome(autor.getNome())
                .autorUnidade(autor.getUnidade())
                .comentario(request.getComentario().trim())
                .dia(LocalDate.now())
                .hora(LocalTime.now())
                .build();
        entityManager.persist(entity);

        return mapToDTO(entity);
    }

    // Mais recente primeiro -- mesma convenção já usada em mensagens/experiências.
    @Transactional
    public List<FotoComentarioDTO> buscaComentariosPorFoto(Long fotoId) {
        return entityManager.createQuery(
                        "SELECT c FROM FotoComentarioEntity c " +
                                "WHERE c.fotoId = :fotoId " +
                                "ORDER BY c.dia DESC, c.hora DESC",
                        FotoComentarioEntity.class)
                .setParameter("fotoId", fotoId)
                .getResultList()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // Usado só pra montar o "sinal" de "tem comentário" na grade de fotos (ver
    // FotoMissionarioService) -- sem carregar o conteúdo dos comentários em si, só quais ids de
    // foto aparecem em pelo menos uma linha de foto_comentario. Uma única query em lote, sem
    // N+1.
    @Transactional
    public Set<Long> fotosComComentario(List<Long> fotoIds) {
        if (fotoIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(entityManager.createQuery(
                        "SELECT DISTINCT c.fotoId FROM FotoComentarioEntity c WHERE c.fotoId IN :ids",
                        Long.class)
                .setParameter("ids", fotoIds)
                .getResultList());
    }

    private FotoComentarioDTO mapToDTO(FotoComentarioEntity entity) {
        return FotoComentarioDTO.builder()
                .id(entity.getId())
                .fotoId(entity.getFotoId())
                .autorTipo(entity.getAutorTipo())
                .autorNome(entity.getAutorNome())
                .autorUnidade(entity.getAutorUnidade())
                .comentario(entity.getComentario())
                .dia(entity.getDia())
                .hora(entity.getHora())
                .build();
    }

}
