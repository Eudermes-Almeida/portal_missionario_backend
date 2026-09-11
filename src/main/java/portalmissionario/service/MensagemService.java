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

@ApplicationScoped
public class MensagemService {

    private static final String TIPO_MEMBRO = "MEMBRO";
    private static final String TIPO_MISSIONARIO = "MISSIONARIO";
    private static final int MENSAGEM_MAX_CARACTERES = 1000;

    @Inject
    EntityManager entityManager;

    // Passo simples (1a etapa): so persiste e devolve a mensagem gravada. Reacao, thread de
    // resposta e "lida" ficam pra depois -- aqui mensagemPaiId so passa direto se informado
    // (sem validar se o pai existe) e lida sempre nasce false.
    @Transactional
    public MensagemDTO enviarMensagem(EnviarMensagemRequestDTO request) {
        if (request == null || request.getMensagem() == null || request.getMensagem().trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem é obrigatória.");
        }
        if (request.getMensagem().length() > MENSAGEM_MAX_CARACTERES) {
            throw new IllegalArgumentException("A mensagem excede o limite de " + MENSAGEM_MAX_CARACTERES + " caracteres.");
        }

        String[] remetente = resolveNomeUnidade(request.getRemetenteTipo(), request.getRemetenteId(), "remetente");
        String[] destinatario = resolveNomeUnidade(request.getDestinatarioTipo(), request.getDestinatarioId(), "destinatário");

        MensagemEntity.MensagemEntityBuilder builder = MensagemEntity.builder()
                .mensagemPaiId(request.getMensagemPaiId())
                .remetenteTipo(request.getRemetenteTipo())
                .remetenteNome(remetente[0])
                .remetenteUnidade(remetente[1])
                .destinatarioTipo(request.getDestinatarioTipo())
                .destinatarioNome(destinatario[0])
                .destinatarioUnidade(destinatario[1])
                .mensagem(request.getMensagem().trim())
                .dia(LocalDate.now())
                .hora(LocalTime.now())
                .lida(false);

        if (TIPO_MEMBRO.equals(request.getRemetenteTipo())) {
            builder.remetenteMembroId(request.getRemetenteId());
        } else {
            builder.remetenteMissionarioId(request.getRemetenteId());
        }

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
