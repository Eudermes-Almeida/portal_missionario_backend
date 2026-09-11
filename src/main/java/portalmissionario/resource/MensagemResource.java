package portalmissionario.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
import portalmissionario.service.MensagemService;

@Path("/mensagens")
public class MensagemResource {

    @Inject
    MensagemService mensagemService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Enviar Mensagem", description = "Persiste uma mensagem entre membro e missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Mensagem gravada", content = @Content(schema = @Schema(implementation = MensagemDTO.class))),
            @APIResponse(responseCode = "400", description = "Dados inválidos (tipo/id de remetente ou destinatário, ou mensagem vazia/muito longa)"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Envia uma mensagem", description = "remetenteTipo/destinatarioTipo devem ser MEMBRO ou MISSIONARIO; remetenteId/destinatarioId são o id na tabela correspondente (matriz_acesso ou dadosmissionarios).")
    public Response enviarMensagem(EnviarMensagemRequestDTO request) {
        try {
            if (request == null
                    || request.getRemetenteTipo() == null || request.getRemetenteTipo().trim().isEmpty()
                    || request.getRemetenteId() == null
                    || request.getDestinatarioTipo() == null || request.getDestinatarioTipo().trim().isEmpty()
                    || request.getDestinatarioId() == null
                    || request.getMensagem() == null || request.getMensagem().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe remetenteTipo, remetenteId, destinatarioTipo, destinatarioId e mensagem.")
                        .build();
            }

            MensagemDTO mensagem = mensagemService.enviarMensagem(request);

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
    public Response buscaMensagemPorId(@PathParam("id") Long id) {
        try {
            MensagemDTO mensagem = mensagemService.buscaMensagemPorId(id);
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

}
