package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.EscreverComentarioRequestDTO;
import portalmissionario.dto.ExperienciaComentarioDTO;
import portalmissionario.entity.ExperienciaComentarioEntity;
import portalmissionario.entity.ExperienciaEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Comentario numa experiencia -- copia de FotoComentarioService (autor_nome/autor_unidade
// congelados no momento do envio, autor sempre o membro dono do token, nunca vem no corpo).
// Multiplos comentarios por pessoa na mesma experiencia sao permitidos.
@ApplicationScoped
public class ExperienciaComentarioService {

    private static final String TIPO_MEMBRO = "MEMBRO";
    private static final int COMENTARIO_MAX_CARACTERES = 100;

    @Inject
    EntityManager entityManager;

    @Transactional
    public ExperienciaComentarioDTO escreverComentario(Long experienciaId, Long autorMembroId, EscreverComentarioRequestDTO request) {
        if (request == null || request.getComentario() == null || request.getComentario().trim().isEmpty()) {
            throw new IllegalArgumentException("O comentário é obrigatório.");
        }
        if (request.getComentario().length() > COMENTARIO_MAX_CARACTERES) {
            throw new IllegalArgumentException("O comentário excede o limite de " + COMENTARIO_MAX_CARACTERES + " caracteres.");
        }

        ExperienciaEntity experiencia = entityManager.find(ExperienciaEntity.class, experienciaId);
        if (experiencia == null) {
            throw new NotFoundException("Experiência não encontrada: " + experienciaId);
        }

        MatrizAcessoEntity autor = entityManager.find(MatrizAcessoEntity.class, autorMembroId);
        if (autor == null) {
            throw new IllegalArgumentException("Membro não encontrado: " + autorMembroId);
        }

        ExperienciaComentarioEntity entity = ExperienciaComentarioEntity.builder()
                .experienciaId(experienciaId)
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

    // Mais recente primeiro -- mesma convenção já usada em mensagens/fotos.
    @Transactional
    public List<ExperienciaComentarioDTO> buscaComentariosPorExperiencia(Long experienciaId) {
        return entityManager.createQuery(
                        "SELECT c FROM ExperienciaComentarioEntity c " +
                                "WHERE c.experienciaId = :experienciaId " +
                                "ORDER BY c.dia DESC, c.hora DESC",
                        ExperienciaComentarioEntity.class)
                .setParameter("experienciaId", experienciaId)
                .getResultList()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // Usado só pra mostrar a contagem no botão "Comentários" da lista de experiências (pedido
    // do usuário, 2026-09-14, depois de reportar que não achava um comentário já gravado --
    // sem a contagem visível, o painel colapsado por padrão escondia o conteúdo). Uma única
    // query em lote, sem N+1 -- mesmo padrão de resumoPorExperiencias em ExperienciaReacaoService.
    @Transactional
    public Map<Long, Long> quantidadePorExperiencias(List<Long> experienciaIds) {
        if (experienciaIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Object[]> linhas = entityManager.createQuery(
                        "SELECT c.experienciaId, COUNT(c) FROM ExperienciaComentarioEntity c " +
                                "WHERE c.experienciaId IN :ids GROUP BY c.experienciaId",
                        Object[].class)
                .setParameter("ids", experienciaIds)
                .getResultList();

        Map<Long, Long> quantidades = new HashMap<>();
        for (Object[] linha : linhas) {
            quantidades.put((Long) linha[0], (Long) linha[1]);
        }
        return quantidades;
    }

    private ExperienciaComentarioDTO mapToDTO(ExperienciaComentarioEntity entity) {
        return ExperienciaComentarioDTO.builder()
                .id(entity.getId())
                .experienciaId(entity.getExperienciaId())
                .autorTipo(entity.getAutorTipo())
                .autorNome(entity.getAutorNome())
                .autorUnidade(entity.getAutorUnidade())
                .comentario(entity.getComentario())
                .dia(entity.getDia())
                .hora(entity.getHora())
                .build();
    }

}
