package portalmissionario.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
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
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import portalmissionario.dto.FotoMissionarioDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.FotoMissionarioService;
import portalmissionario.service.SessaoService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

// Mesma tratativa de ExperienciaResource (escrever + ler), so que "escrever" aqui e subir um
// arquivo de imagem (multipart) em vez de mandar um JSON com texto.
@Path("/fotos")
public class FotoMissionarioResource {

    private static final String HEADER_TOKEN = "X-Auth-Token";

    @Inject
    FotoMissionarioService fotoMissionarioService;

    @Inject
    SessaoService sessaoService;

    // Bean de formulario multipart -- so o campo "arquivo" (a imagem em si). O tamanho maximo
    // aceito e limitado a nivel de servidor por quarkus.http.limits.max-body-size
    // (application.properties): a compressao client-side (Canvas, ver conversa sobre
    // estrategia) deve gerar ~250-350kb, esse limite e so uma rede de seguranca contra
    // abuso/bug, nao um teto pensado pra caber a foto original sem compressao.
    public static class FotoUploadForm {
        @RestForm("arquivo")
        public FileUpload arquivo;
    }

    @POST
    @Path("/missionario/{missionarioId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Subir Foto", description = "Sobe uma foto pra galeria pública do missionário (repassada pro Supabase Storage)")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Foto gravada", content = @Content(schema = @Schema(implementation = FotoMissionarioDTO.class))),
            @APIResponse(responseCode = "400", description = "Nenhum arquivo enviado, ou formato inválido (só JPEG)"),
            @APIResponse(responseCode = "401", description = "Sessão inválida ou expirada"),
            @APIResponse(responseCode = "403", description = "O membro logado não é o próprio missionário deste perfil"),
            @APIResponse(responseCode = "404", description = "Missionário não encontrado"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor (inclui falha ao enviar pro Supabase Storage)"),
    })
    @Operation(summary = "Sobe uma foto", description = "Campo do form multipart: 'arquivo' (JPEG). Só o próprio missionário (membro logado com o mesmo registromembro do perfil) pode subir.")
    public Response subirFoto(@HeaderParam(HEADER_TOKEN) String token, @PathParam("missionarioId") Long missionarioId, @BeanParam FotoUploadForm form) {
        try {
            if (form == null || form.arquivo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Selecione uma foto.")
                        .build();
            }

            Optional<MatrizAcessoEntity> autorOpt = sessaoService.buscaMembroPorToken(token);
            if (autorOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessão inválida ou expirada. Faça login novamente.")
                        .build();
            }

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(form.arquivo.uploadedFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            FotoMissionarioDTO foto = fotoMissionarioService.subirFoto(
                    missionarioId, autorOpt.get().getId(), bytes, form.arquivo.contentType());

            return Response.ok(foto).build();
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
                    .entity("Erro ao subir foto: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/missionario/{missionarioId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Galeria do Missionário", description = "Lista as fotos públicas de um missionário")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Fotos do missionário, mais recente primeiro (lista vazia se não houver nenhuma)", content = @Content(schema = @Schema(implementation = FotoMissionarioDTO.class))),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Lista fotos de um missionário")
    public Response buscaFotosPorMissionario(@PathParam("missionarioId") Long missionarioId) {
        try {
            List<FotoMissionarioDTO> fotos = fotoMissionarioService.buscaFotosPorMissionario(missionarioId);
            return Response.ok(fotos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar fotos: " + e.getMessage())
                    .build();
        }
    }

}
