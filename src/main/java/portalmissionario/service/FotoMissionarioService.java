package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.FotoMissionarioDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.FotoMissionarioEntity;
import portalmissionario.entity.MatrizAcessoEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// Galeria de fotos do missionario -- mesma tratativa de ExperienciaService (autor e sempre o
// proprio missionario-alvo), so que aqui o "conteudo" e um arquivo de imagem em vez de texto,
// repassado pro Supabase Storage antes de persistir a linha.
@ApplicationScoped
public class FotoMissionarioService {

    private static final String CONTENT_TYPE_JPEG = "image/jpeg";

    // TODO-DES (temporário, pedido do usuário 2026-09-12, mesmo padrão da flag em
    // ExperienciaService/DadosMissionariosService): restrição "só o próprio missionário sobe
    // foto" desativada pra facilitar teste em DES. REATIVAR (voltar pra `true`) antes de sair
    // do ambiente de DES.
    private static final boolean RESTRICAO_PROPRIO_MISSIONARIO_ATIVA = false;

    @Inject
    EntityManager entityManager;

    @Inject
    SupabaseStorageService supabaseStorageService;

    // membroAutorId e o dono do token da sessao (resolvido no FotoMissionarioResource via
    // SessaoService, nunca vem no corpo). "bytes"/"contentType" ja chegam do multipart do
    // Resource -- este service so valida, sobe pro Storage e persiste.
    @Transactional
    public FotoMissionarioDTO subirFoto(Long missionarioId, Long membroAutorId, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Selecione uma foto.");
        }
        if (!CONTENT_TYPE_JPEG.equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("Formato de imagem inválido. Só é aceito JPEG.");
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
                throw new ForbiddenException("Somente o próprio missionário pode subir fotos no seu perfil.");
            }
        }

        String caminho = "galeria/" + missionarioId + "/" + UUID.randomUUID() + ".jpg";
        String url = supabaseStorageService.subir(caminho, bytes, CONTENT_TYPE_JPEG);

        FotoMissionarioEntity entity = FotoMissionarioEntity.builder()
                .missionarioId(missionarioId)
                .url(url)
                .caminhoStorage(caminho)
                .dia(LocalDate.now())
                .hora(LocalTime.now())
                .build();
        entityManager.persist(entity);

        return mapToDTO(entity);
    }

    // Galeria pública do missionário, mais recente primeiro -- qualquer membro logado pode ver.
    @Transactional
    public List<FotoMissionarioDTO> buscaFotosPorMissionario(Long missionarioId) {
        return entityManager.createQuery(
                        "SELECT f FROM FotoMissionarioEntity f " +
                                "WHERE f.missionarioId = :id " +
                                "ORDER BY f.dia DESC, f.hora DESC",
                        FotoMissionarioEntity.class)
                .setParameter("id", missionarioId)
                .getResultList()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private FotoMissionarioDTO mapToDTO(FotoMissionarioEntity entity) {
        return FotoMissionarioDTO.builder()
                .id(entity.getId())
                .missionarioId(entity.getMissionarioId())
                .url(entity.getUrl())
                .dia(entity.getDia())
                .hora(entity.getHora())
                .build();
    }

}
