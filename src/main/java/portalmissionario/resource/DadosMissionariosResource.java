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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import portalmissionario.dto.DadosMissionariosDTO;
import portalmissionario.dto.EnviarEmailRequestDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.DadosMissionariosService;
import portalmissionario.service.EnvioEmailService;
import portalmissionario.service.SessaoService;

import java.util.List;
import java.util.Optional;

@Path("/dadosmissionarios")
public class DadosMissionariosResource {

    private static final String HEADER_TOKEN = "X-Auth-Token";

    @Inject
    DadosMissionariosService dadosMissionariosService;

    @Inject
    EnvioEmailService envioEmailService;

    @Inject
    SessaoService sessaoService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Busca Dados Missionários", description = "Busca missionários (dados atuais) por unidade")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Missionários encontrados (pode ser lista vazia se a unidade não tiver registros)", content = @Content(schema = @Schema(implementation = DadosMissionariosDTO.class))),
            @APIResponse(responseCode = "400", description = "Parâmetro 'unidade' não informado", content = @Content(schema = @Schema(implementation = DadosMissionariosDTO.class))),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = DadosMissionariosDTO.class))),
    })
    @Operation(summary = "Busca missionários por unidade", description = "Busca os missionários (dados atuais, tabela dadosmissionarios) de uma unidade específica. Informar 'Estaca Betim' retorna os dados de todas as unidades, já que a estaca é a soma de todas as alas/ramos.")
    public Response buscaDadosMissionariosPorUnidade(@HeaderParam(HEADER_TOKEN) String token, @QueryParam("unidade") String unidade) {
        try {
            if (unidade == null || unidade.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("O parâmetro 'unidade' é obrigatório")
                        .build();
            }

            // registromembro do membro logado (se houver sessão válida) só serve pra calcular
            // "podeEscreverExperiencia" de cada missionário -- ver DadosMissionariosService.
            String registromembroLogado = sessaoService.buscaMembroPorToken(token)
                    .map(MatrizAcessoEntity::getRegistromembro)
                    .orElse(null);

            List<DadosMissionariosDTO> dadosMissionarios = dadosMissionariosService.buscaDadosMissionariosPorUnidade(unidade, registromembroLogado);

            return Response.ok(dadosMissionarios).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar missionários: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{id}/email")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = "Enviar Email", description = "Dispara um email de verdade pro missionário (Quarkus Mailer)")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Email enviado"),
            @APIResponse(responseCode = "400", description = "Mensagem vazia/muito longa, email do remetente inválido, ou missionário sem email cadastrado"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "404", description = "Missionário não encontrado"),
            @APIResponse(responseCode = "500", description = "Erro ao enviar o email (falha no provedor SMTP)"),
    })
    @Operation(summary = "Envia um email pro missionário", description = "O remetente (nome/unidade no rodapé do email) é sempre o membro dono do token em X-Auth-Token. emailRemetente é opcional e, se informado, vira Reply-To -- nunca é persistido.")
    public Response enviarEmail(@HeaderParam(HEADER_TOKEN) String token, @PathParam("id") Long id, EnviarEmailRequestDTO request) {
        try {
            if (request == null || request.getMensagem() == null || request.getMensagem().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe a mensagem.")
                        .build();
            }

            Optional<MatrizAcessoEntity> remetenteOpt = sessaoService.buscaMembroPorToken(token);
            if (remetenteOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessão inválida ou expirada. Faça login novamente.")
                        .build();
            }

            MatrizAcessoEntity remetente = remetenteOpt.get();
            envioEmailService.enviarEmailParaMissionario(
                    id, remetente.getNome(), remetente.getUnidade(), request.getMensagem(), request.getEmailRemetente());

            return Response.noContent().build();
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
                    .entity("Erro ao enviar email: " + e.getMessage())
                    .build();
        }
    }

}
