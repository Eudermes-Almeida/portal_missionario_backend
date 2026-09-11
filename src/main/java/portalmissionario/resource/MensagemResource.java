package portalmissionario.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import portalmissionario.dto.EnviarMensagemRequestDTO;
import portalmissionario.dto.MensagemDTO;
import portalmissionario.dto.ReagirRequestDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.MensagemReacaoService;
import portalmissionario.service.MensagemService;
import portalmissionario.service.SessaoService;

import java.util.List;
import java.util.Optional;

@Path("/mensagens")
public class MensagemResource {

    private static final String HEADER_TOKEN = "X-Auth-Token";

    @Inject
    MensagemService mensagemService;

    @Inject
    MensagemReacaoService mensagemReacaoService;

    @Inject
    SessaoService sessaoService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Enviar Mensagem", description = "Persiste uma mensagem pública de um membro no perfil de um missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Mensagem gravada", content = @Content(schema = @Schema(implementation = MensagemDTO.class))),
            @APIResponse(responseCode = "400", description = "Dados inválidos (tipo/id de destinatário, ou mensagem vazia/muito longa)"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Envia uma mensagem", description = "O remetente é sempre o membro dono do token em X-Auth-Token. destinatarioTipo deve ser MEMBRO ou MISSIONARIO; destinatarioId é o id na tabela correspondente (matriz_acesso ou dadosmissionarios).")
    public Response enviarMensagem(@HeaderParam(HEADER_TOKEN) String token, EnviarMensagemRequestDTO request) {
        try {
            if (request == null
                    || request.getDestinatarioTipo() == null || request.getDestinatarioTipo().trim().isEmpty()
                    || request.getDestinatarioId() == null
                    || request.getMensagem() == null || request.getMensagem().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe destinatarioTipo, destinatarioId e mensagem.")
                        .build();
            }

            // AutorizacaoFilter ja garantiu que o token e valido antes de chegar aqui --
            // resolver de novo so pra pegar o id do membro (o filtro nao guarda a entidade
            // pra reuso entre filtro e resource).
            Optional<MatrizAcessoEntity> remetenteOpt = sessaoService.buscaMembroPorToken(token);
            if (remetenteOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessão inválida ou expirada. Faça login novamente.")
                        .build();
            }

            MensagemDTO mensagem = mensagemService.enviarMensagem(remetenteOpt.get().getId(), request);

            return Response.ok(mensagem).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao enviar mensagem: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Busca Mensagem", description = "Busca uma mensagem pelo id")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Mensagem encontrada", content = @Content(schema = @Schema(implementation = MensagemDTO.class))),
            @APIResponse(responseCode = "404", description = "Mensagem não encontrada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Busca mensagem por id")
    public Response buscaMensagemPorId(@HeaderParam(HEADER_TOKEN) String token, @PathParam("id") Long id) {
        try {
            Long membroIdAtual = sessaoService.buscaMembroPorToken(token).map(MatrizAcessoEntity::getId).orElse(null);
            MensagemDTO mensagem = mensagemService.buscaMensagemPorId(id, membroIdAtual);
            return Response.ok(mensagem).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar mensagem: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/missionario/{missionarioId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Mural do Missionário", description = "Lista as mensagens públicas recebidas por um missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Mensagens do missionário, mais recente primeiro (lista vazia se não houver nenhuma)", content = @Content(schema = @Schema(implementation = MensagemDTO.class))),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Lista mensagens de um missionário")
    public Response buscaMensagensPorMissionario(@HeaderParam(HEADER_TOKEN) String token, @PathParam("missionarioId") Long missionarioId) {
        try {
            Long membroIdAtual = sessaoService.buscaMembroPorToken(token).map(MatrizAcessoEntity::getId).orElse(null);
            List<MensagemDTO> mensagens = mensagemService.buscaMensagensPorMissionario(missionarioId, membroIdAtual);
            return Response.ok(mensagens).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar mensagens: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{id}/reacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Manifestar", description = "Registra, troca ou remove (toggle) a reação do membro logado a uma mensagem")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Reação aplicada — devolve o resumo atualizado (tipo + quantidade) e a reação atual do próprio membro (null se removida)"),
            @APIResponse(responseCode = "400", description = "tipoReacao inválido ou ausente"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "404", description = "Mensagem não encontrada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Reage (ou troca/remove a reação) a uma mensagem", description = "O autor é sempre o membro dono do token em X-Auth-Token. Clicar de novo no mesmo tipo remove a reação (toggle); clicar em outro tipo troca.")
    public Response reagir(@HeaderParam(HEADER_TOKEN) String token, @PathParam("id") Long id, ReagirRequestDTO request) {
        try {
            if (request == null || request.getTipoReacao() == null || request.getTipoReacao().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe tipoReacao.")
                        .build();
            }

            Optional<MatrizAcessoEntity> autorOpt = sessaoService.buscaMembroPorToken(token);
            if (autorOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessão inválida ou expirada. Faça login novamente.")
                        .build();
            }

            MensagemReacaoService.ResultadoReacao resultado = mensagemReacaoService.reagir(
                    id, autorOpt.get().getId(), request.getTipoReacao().trim().toUpperCase());

            return Response.ok(new ReacaoRespostaDTO(resultado.resumo(), resultado.minhaReacao())).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao registrar reação: " + e.getMessage())
                    .build();
        }
    }

    // Corpo de resposta do endpoint de reação -- não é um MensagemDTO completo de proposito,
    // pra nao reenviar texto/remetente/etc. a cada clique no seletor de icones.
    public record ReacaoRespostaDTO(List<portalmissionario.dto.ReacaoResumoDTO> reacoes, String minhaReacao) {
    }

    @GET
    @Path("/{id}/reacao/{tipo}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Quem Reagiu", description = "Lista quem reagiu com um determinado ícone numa mensagem")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Lista de autores (nome + unidade), mais recente primeiro"),
            @APIResponse(responseCode = "400", description = "tipo inválido"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Lista quem reagiu com um ícone", description = "Usado pelo popover que abre ao clicar num pill de reação no mural.")
    public Response listaAutoresReacao(@PathParam("id") Long id, @PathParam("tipo") String tipo) {
        try {
            List<portalmissionario.dto.ReacaoAutorDTO> autores = mensagemReacaoService.listaAutores(id, tipo.trim().toUpperCase());
            return Response.ok(autores).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao listar quem reagiu: " + e.getMessage())
                    .build();
        }
    }

}
