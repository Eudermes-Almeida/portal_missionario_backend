package portalmissionario.resource;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import portalmissionario.dto.LoginRequestDTO;
import portalmissionario.dto.LoginResponseDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.MatrizAcessoService;
import portalmissionario.service.SessaoService;

import java.util.Optional;

// Login "normal" -- nao confundir com /matriz-acesso/identificar (fluxo de primeiro acesso,
// MatrizAcessoResource).
@Path("/auth")
public class AuthResource {

    @Inject
    MatrizAcessoService matrizAcessoService;

    @Inject
    SessaoService sessaoService;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Login", description = "Login com login+senha já cadastrados, cria uma sessão")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Login válido, sessão criada", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @APIResponse(responseCode = "400", description = "Login/senha não informados"),
            @APIResponse(responseCode = "401", description = "Login ou senha inválidos"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Login de um membro já cadastrado", description = "Retorna um token opaco (header X-Auth-Token nas chamadas seguintes) junto com nome/unidade/escopo do membro.")
    public Response login(LoginRequestDTO request) {
        try {
            if (request == null || request.getLogin() == null || request.getLogin().trim().isEmpty()
                    || request.getSenha() == null || request.getSenha().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe login e senha.")
                        .build();
            }

            Optional<MatrizAcessoEntity> membroOpt = matrizAcessoService.buscaPorLogin(request.getLogin());

            // Mesma mensagem/status tanto pra "login não existe" quanto "senha errada" --
            // não revelar qual dos dois motivos causou a falha.
            if (membroOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Login ou senha inválidos.")
                        .build();
            }

            MatrizAcessoEntity membro = membroOpt.get();
            if (membro.getSenhaHash() == null || !BcryptUtil.matches(request.getSenha(), membro.getSenhaHash())) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Login ou senha inválidos.")
                        .build();
            }

            String token = sessaoService.criarSessao(membro.getId());

            LoginResponseDTO resposta = LoginResponseDTO.builder()
                    .token(token)
                    .nome(membro.getNome())
                    .unidade(membro.getUnidade())
                    .escopo(membro.getEscopo())
                    .build();

            return Response.ok(resposta).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao efetuar login: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @Tag(name = "Logout", description = "Encerra a sessão do token informado")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Sessão encerrada (idempotente -- mesmo se o token já não existir)"),
    })
    @Operation(summary = "Logout", description = "Apaga a sessão referente ao token no header X-Auth-Token.")
    public Response logout(@HeaderParam("X-Auth-Token") String token) {
        sessaoService.encerrarSessao(token);
        return Response.ok().build();
    }

}
