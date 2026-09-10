package portalmissionario.filter;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import portalmissionario.entity.MatrizAcessoEntity;
import portalmissionario.service.SessaoService;

import java.util.Optional;
import java.util.Set;

// Filtro central de autorizacao -- roda na frente de TODOS os endpoints da API, sem precisar
// alterar nenhum Resource individualmente. @Provider sem @NameBinding aplica globalmente por
// padrao no JAX-RS/RESTEasy Reactive. Mesmo desenho ja validado em producao no projeto irmao
// RAIO_X_UNIDADE (sessao simples em tabela, nao JWT).
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AutorizacaoFilter implements ContainerRequestFilter {

    private static final String HEADER_TOKEN = "X-Auth-Token";
    private static final String ESCOPO_ESTACA = "ESTACA";

    // Rotas que nao exigem sessao: login em si, e o fluxo de primeiro acesso completo (roda
    // ANTES de existir qualquer token), alem da listagem segura da matriz de acesso
    // (MatrizAcessoDTO ja nao expoe nada sensivel).
    private static final Set<String> CAMINHOS_LIVRES = Set.of(
            "/auth/login",
            "/auth/logout",
            "/matriz-acesso",
            "/matriz-acesso/identificar",
            "/matriz-acesso/cadastrar-credenciais"
    );

    @Inject
    SessaoService sessaoService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Preflight de CORS nao deve exigir sessao -- o navegador nem manda o header
        // customizado nesse pedido.
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        UriInfo uriInfo = requestContext.getUriInfo();
        String caminho = uriInfo.getPath();

        if (CAMINHOS_LIVRES.contains(caminho)) {
            return;
        }

        String token = requestContext.getHeaderString(HEADER_TOKEN);
        Optional<MatrizAcessoEntity> membroOpt = sessaoService.buscaMembroPorToken(token);

        if (membroOpt.isEmpty()) {
            abortar(requestContext, Response.Status.UNAUTHORIZED, "Sessão inválida ou expirada. Faça login novamente.");
            return;
        }

        MatrizAcessoEntity membro = membroOpt.get();
        String unidadeSolicitada = uriInfo.getQueryParameters().getFirst("unidade");

        if (unidadeSolicitada != null) {
            boolean escopoTotal = ESCOPO_ESTACA.equalsIgnoreCase(membro.getEscopo());
            boolean mesmaUnidade = membro.getUnidade() != null
                    && membro.getUnidade().equalsIgnoreCase(unidadeSolicitada.trim());

            if (!escopoTotal && !mesmaUnidade) {
                abortar(requestContext, Response.Status.FORBIDDEN, "Você não tem permissão para ver os dados desta unidade.");
            }
        }
    }

    private void abortar(ContainerRequestContext requestContext, Response.Status status, String mensagem) {
        requestContext.abortWith(
                Response.status(status)
                        .entity(mensagem)
                        .type(MediaType.TEXT_PLAIN)
                        .build());
    }
}
