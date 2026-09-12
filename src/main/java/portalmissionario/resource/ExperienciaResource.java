package portalmissionario.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
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
import portalmissionario.dto.EscreverExperienciaRequestDTO;
import portalmissionario.dto.ExperienciaDTO;
import portalmissionario.dto.ReacaoAutorDTO;
import portalmissionario.dto.ReacaoResumoDTO;
import portalmissionario.dto.ReagirRequestDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.ExperienciaReacaoService;
import portalmissionario.service.ExperienciaService;
import portalmissionario.service.SessaoService;

import java.util.List;
import java.util.Optional;

// Mesma tratativa de MensagemResource (escrever + ler + "Manifestar"), so que o autor do
// relato e sempre o proprio missionario-alvo (ver ExperienciaService).
@Path("/experiencias")
public class ExperienciaResource {

    private static final String HEADER_TOKEN = "X-Auth-Token";

    @Inject
    ExperienciaService experienciaService;

    @Inject
    ExperienciaReacaoService experienciaReacaoService;

    @Inject
    SessaoService sessaoService;

    @POST
    @Path("/missionario/{missionarioId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Escrever Experiência", description = "Persiste um relato de experiência missionária, público no perfil do próprio missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Experiência gravada", content = @Content(schema = @Schema(implementation = ExperienciaDTO.class))),
            @APIResponse(responseCode = "400", description = "Experiência vazia ou muito longa"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "403", description = "O membro logado não é o próprio missionário deste perfil"),
            @APIResponse(responseCode = "404", description = "Missionário não encontrado"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Escreve uma experiência", description = "Só o próprio missionário (membro logado com o mesmo registromembro do perfil) pode escrever.")
    public Response escreverExperiencia(@HeaderParam(HEADER_TOKEN) String token, @PathParam("missionarioId") Long missionarioId, EscreverExperienciaRequestDTO request) {
        try {
            if (request == null || request.getExperiencia() == null || request.getExperiencia().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe a experiência.")
                        .build();
            }

            Optional<MatrizAcessoEntity> autorOpt = sessaoService.buscaMembroPorToken(token);
            if (autorOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessão inválida ou expirada. Faça login novamente.")
                        .build();
            }

            ExperienciaDTO experiencia = experienciaService.escreverExperiencia(missionarioId, autorOpt.get().getId(), request);

            return Response.ok(experiencia).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (ForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao escrever experiência: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/missionario/{missionarioId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Experiências do Missionário", description = "Lista os relatos de experiência públicos de um missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Experiências do missionário, mais recente primeiro (lista vazia se não houver nenhuma)", content = @Content(schema = @Schema(implementation = ExperienciaDTO.class))),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Lista experiências de um missionário")
    public Response buscaExperienciasPorMissionario(@HeaderParam(HEADER_TOKEN) String token, @PathParam("missionarioId") Long missionarioId) {
        try {
            Long membroIdAtual = sessaoService.buscaMembroPorToken(token).map(MatrizAcessoEntity::getId).orElse(null);
            List<ExperienciaDTO> experiencias = experienciaService.buscaExperienciasPorMissionario(missionarioId, membroIdAtual);
            return Response.ok(experiencias).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar experiências: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/{id}/reacao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Manifestar", description = "Registra, troca ou remove (toggle) a reação do membro logado a uma experiência")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Reação aplicada — devolve o resumo atualizado (tipo + quantidade) e a reação atual do próprio membro (null se removida)"),
            @APIResponse(responseCode = "400", description = "tipoReacao inválido ou ausente"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "404", description = "Experiência não encontrada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Reage (ou troca/remove a reação) a uma experiência", description = "O autor é sempre o membro dono do token em X-Auth-Token. Clicar de novo no mesmo tipo remove a reação (toggle); clicar em outro tipo troca.")
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

            ExperienciaReacaoService.ResultadoReacao resultado = experienciaReacaoService.reagir(
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

    public record ReacaoRespostaDTO(List<ReacaoResumoDTO> reacoes, String minhaReacao) {
    }

    @GET
    @Path("/{id}/reacao/{tipo}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Quem Reagiu", description = "Lista quem reagiu com um determinado ícone numa experiência")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Lista de autores (nome + unidade), mais recente primeiro"),
            @APIResponse(responseCode = "400", description = "tipo inválido"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Lista quem reagiu com um ícone", description = "Usado pelo popover que abre ao clicar num pill de reação na lista de experiências.")
    public Response listaAutoresReacao(@PathParam("id") Long id, @PathParam("tipo") String tipo) {
        try {
            List<ReacaoAutorDTO> autores = experienciaReacaoService.listaAutores(id, tipo.trim().toUpperCase());
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
