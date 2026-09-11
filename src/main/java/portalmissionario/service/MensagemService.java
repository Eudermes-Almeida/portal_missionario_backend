package portalmissionario.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.dto.EnviarMensagemRequestDTO;
import portalmissionario.dto.MensagemDTO;
import portalmissionario.entity.DadosMissionariosEntity;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.entity.MensagemEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class MensagemService {

    private static final String TIPO_MEMBRO = "MEMBRO";
    private static final String TIPO_MISSIONARIO = "MISSIONARIO";
    private static final int MENSAGEM_MAX_CARACTERES = 1000;

    @Inject
    EntityManager entityManager;

    // Passo simples (1a etapa): so persiste e devolve a mensagem gravada. Reacao e thread de
    // resposta ficam pra depois -- aqui mensagemPaiId so passa direto se informado (sem
    // validar se o pai existe).
    //
    // remetente sempre e o MEMBRO dono do token da sessao (resolvido no MensagemResource via
    // SessaoService, nao vem no corpo da requisicao) -- ninguem pode mandar mensagem se
    // passando por outro membro. "lida" nasce false pq a mensagem e publica no perfil do
    // missionario (mural), entao so faz sentido controlar leitura do lado do proprio
    // missionario, nao do membro que escreveu.
    @Transactional
    public MensagemDTO enviarMensagem(Long remetenteMembroId, EnviarMensagemRequestDTO request) {
        if (request == null || request.getMensagem() == null || request.getMensagem().trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem é obrigatória.");
        }
        if (request.getMensagem().length() > MENSAGEM_MAX_CARACTERES) {
            throw new IllegalArgumentException("A mensagem excede o limite de " + MENSAGEM_MAX_CARACTERES + " caracteres.");
        }

        String[] remetente = resolveNomeUnidade(TIPO_MEMBRO, remetenteMembroId, "remetente");
        String[] destinatario = resolveNomeUnidade(request.getDestinatarioTipo(), request.getDestinatarioId(), "destinatário");

        MensagemEntity.MensagemEntityBuilder builder = MensagemEntity.builder()
                .mensagemPaiId(request.getMensagemPaiId())
                .remetenteTipo(TIPO_MEMBRO)
                .remetenteMembroId(remetenteMembroId)
                .remetenteNome(remetente[0])
                .remetenteUnidade(remetente[1])
                .destinatarioTipo(request.getDestinatarioTipo())
                .destinatarioNome(destinatario[0])
                .destinatarioUnidade(destinatario[1])
                .mensagem(request.getMensagem().trim())
                .dia(LocalDate.now())
                .hora(LocalTime.now())
                .lida(false);

        if (TIPO_MEMBRO.equals(request.getDestinatarioTipo())) {
            builder.destinatarioMembroId(request.getDestinatarioId());
        } else {
            builder.destinatarioMissionarioId(request.getDestinatarioId());
        }

        MensagemEntity entity = builder.build();
        entityManager.persist(entity);

        return mapToDTO(entity);
    }

    @Transactional
    public MensagemDTO buscaMensagemPorId(Long id) {
        MensagemEntity entity = entityManager.find(MensagemEntity.class, id);
        if (entity == null) {
            throw new NotFoundException("Mensagem não encontrada: " + id);
        }
        return mapToDTO(entity);
    }

    // Mural público do missionário -- todas as mensagens que ele recebeu, mais recente
    // primeiro. Qualquer membro logado pode ver (mensagem é pública, ver
    // 008_create_table_mensagens.sql), não só quem escreveu.
    @Transactional
    public List<MensagemDTO> buscaMensagensPorMissionario(Long missionarioId) {
        List<MensagemEntity> entidades = entityManager.createQuery(
                        "SELECT m FROM MensagemEntity m " +
                                "WHERE m.destinatarioTipo = :tipo AND m.destinatarioMissionarioId = :id " +
                                "ORDER BY m.dia DESC, m.hora DESC",
                        MensagemEntity.class)
                .setParameter("tipo", TIPO_MISSIONARIO)
                .setParameter("id", missionarioId)
                .getResultList();

        return entidades.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Resolve nome/unidade do lado (remetente ou destinatario) na tabela certa conforme o
    // tipo, pra "congelar" esses dois campos na mensagem no momento do envio (ver comentario
    // em 008_create_table_mensagens.sql). Retorna [nome, unidade].
    private String[] resolveNomeUnidade(String tipo, Long id, String rotulo) {
        if (id == null) {
            throw new IllegalArgumentException("O id do " + rotulo + " é obrigatório.");
        }

        if (TIPO_MEMBRO.equals(tipo)) {
            MatrizAcessoEntity membro = entityManager.find(MatrizAcessoEntity.class, id);
            if (membro == null) {
                throw new IllegalArgumentException("Membro (" + rotulo + ") não encontrado: " + id);
            }
            return new String[]{membro.getNome(), membro.getUnidade()};
        }

        if (TIPO_MISSIONARIO.equals(tipo)) {
            DadosMissionariosEntity missionario = entityManager.find(DadosMissionariosEntity.class, id);
            if (missionario == null) {
                throw new IllegalArgumentException("Missionário (" + rotulo + ") não encontrado: " + id);
            }
            return new String[]{missionario.getNomemissao(), missionario.getUnidade()};
        }

        throw new IllegalArgumentException("Tipo de " + rotulo + " inválido (esperado MEMBRO ou MISSIONARIO): " + tipo);
    }

    private MensagemDTO mapToDTO(MensagemEntity entity) {
        Long remetenteId = TIPO_MEMBRO.equals(entity.getRemetenteTipo())
                ? entity.getRemetenteMembroId()
                : entity.getRemetenteMissionarioId();
        Long destinatarioId = TIPO_MEMBRO.equals(entity.getDestinatarioTipo())
                ? entity.getDestinatarioMembroId()
                : entity.getDestinatarioMissionarioId();

        return MensagemDTO.builder()
                .id(entity.getId())
                .mensagemPaiId(entity.getMensagemPaiId())
                .remetenteTipo(entity.getRemetenteTipo())
                .remetenteId(remetenteId)
                .remetenteNome(entity.getRemetenteNome())
                .remetenteUnidade(entity.getRemetenteUnidade())
                .destinatarioTipo(entity.getDestinatarioTipo())
                .destinatarioId(destinatarioId)
                .destinatarioNome(entity.getDestinatarioNome())
                .destinatarioUnidade(entity.getDestinatarioUnidade())
                .mensagem(entity.getMensagem())
                .dia(entity.getDia())
                .hora(entity.getHora())
                .lida(entity.getLida())
                .build();
    }

}
