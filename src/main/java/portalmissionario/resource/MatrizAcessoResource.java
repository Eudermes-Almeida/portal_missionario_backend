package portalmissionario.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
import portalmissionario.dto.CadastroCredenciaisRequestDTO;
import portalmissionario.dto.IdentificacaoRequestDTO;
import portalmissionario.dto.IdentificacaoResponseDTO;
import portalmissionario.dto.MatrizAcessoDTO;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.MatrizAcessoService;

import java.util.List;
import java.util.Optional;

@Path("/matriz-acesso")
public class MatrizAcessoResource {

    @Inject
    MatrizAcessoService matrizAcessoService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Busca Matriz de Acesso", description = "Busca membros autorizados por unidade")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Membros encontrados (pode ser lista vazia se a unidade não tiver registros)", content = @Content(schema = @Schema(implementation = MatrizAcessoDTO.class))),
            @APIResponse(responseCode = "400", description = "Parâmetro 'unidade' não informado", content = @Content(schema = @Schema(implementation = MatrizAcessoDTO.class))),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = MatrizAcessoDTO.class))),
    })
    @Operation(summary = "Busca membros autorizados por unidade", description = "Informar 'Estaca Betim' retorna os membros de todas as unidades. Campos sensíveis (registromembro, data de nascimento, senha) não são retornados por este endpoint.")
    public Response buscaMembrosPorUnidade(@QueryParam("unidade") String unidade) {
        try {
            if (unidade == null || unidade.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("O parâmetro 'unidade' é obrigatório")
                        .build();
            }

            List<MatrizAcessoDTO> membros = matrizAcessoService.buscaMembrosPorUnidade(unidade);

            return Response.ok(membros).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao buscar membros: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/identificar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Identificação de Membro", description = "Primeiro passo do primeiro acesso: identifica o membro por nascimento + últimos 4 caracteres do registro de membro")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Membro encontrado e ainda sem login/senha cadastrados", content = @Content(schema = @Schema(implementation = IdentificacaoResponseDTO.class))),
            @APIResponse(responseCode = "400", description = "Dados de identificação não informados"),
            @APIResponse(responseCode = "404", description = "Não encontrado, ou já cadastrado anteriormente — mesma mensagem para os dois casos, de propósito"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Identifica um membro pelo nascimento + registro de membro", description = "Não distingue 'não encontrado' de 'já cadastrado' na resposta, para não revelar qual dos dois motivos causou a falha.")
    public Response identificar(IdentificacaoRequestDTO request) {
        try {
            if (request == null || request.getNascimento() == null
                    || request.getUltimosQuatroRegistro() == null || request.getUltimosQuatroRegistro().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe a data de nascimento e os últimos 4 caracteres do registro de membro.")
                        .build();
            }

            Optional<MatrizAcessoEntity> encontrado = matrizAcessoService.buscaPorNascimentoERegistroMembro(
                    request.getNascimento(), request.getUltimosQuatroRegistro());

            boolean jaCadastrado = encontrado.isPresent()
                    && encontrado.get().getLogin() != null
                    && !encontrado.get().getLogin().isBlank();

            if (encontrado.isEmpty() || jaCadastrado) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Procure a Presidência da Estaca.")
                        .build();
            }

            MatrizAcessoEntity membro = encontrado.get();
            IdentificacaoResponseDTO resposta = IdentificacaoResponseDTO.builder()
                    .id(membro.getId())
                    .nome(membro.getNome())
                    .build();

            return Response.ok(resposta).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao identificar membro: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/cadastrar-credenciais")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Cadastro de Credenciais", description = "Segundo passo do primeiro acesso: grava login e senha escolhidos pelo membro já identificado")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Login e senha cadastrados com sucesso"),
            @APIResponse(responseCode = "400", description = "Login/senha não informados ou senha muito curta"),
            @APIResponse(responseCode = "404", description = "Id de membro inexistente"),
            @APIResponse(responseCode = "409", description = "Login já em uso por outro membro, ou este membro já tinha se cadastrado antes"),
            @APIResponse(responseCode = "500", description = "Erro interno do servidor"),
    })
    @Operation(summary = "Cadastra login e senha de um membro já identificado", description = "Só é aceito uma única vez por membro — uma vez que login/senha existem, uma nova tentativa retorna 409, sem reabrir cadastro.")
    public Response cadastrarCredenciais(CadastroCredenciaisRequestDTO request) {
        try {
            if (request == null || request.getId() == null
                    || request.getLogin() == null || request.getLogin().trim().isEmpty()
                    || request.getSenha() == null || request.getSenha().trim().length() < 4) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Informe login e uma senha com pelo menos 4 caracteres.")
                        .build();
            }

            Optional<MatrizAcessoEntity> membroOpt = matrizAcessoService.buscaMembroPorId(request.getId());
            if (membroOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Procure a Presidência da Estaca.")
                        .build();
            }

            MatrizAcessoEntity membro = membroOpt.get();
            if (membro.getLogin() != null && !membro.getLogin().isBlank()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Procure a Presidência da Estaca.")
                        .build();
            }

            if (matrizAcessoService.loginJaExiste(request.getLogin())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Este login já está em uso, escolha outro.")
                        .build();
            }

            matrizAcessoService.cadastrarCredenciais(request.getId(), request.getLogin(), request.getSenha());

            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erro ao cadastrar credenciais: " + e.getMessage())
                    .build();
        }
    }

}
