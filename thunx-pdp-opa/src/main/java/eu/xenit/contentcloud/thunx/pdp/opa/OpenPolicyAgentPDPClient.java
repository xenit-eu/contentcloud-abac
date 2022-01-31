package eu.xenit.contentcloud.thunx.pdp.opa;

import eu.xenit.contentcloud.opa.client.OpaClient;
import eu.xenit.contentcloud.opa.client.api.CompileApi.PartialEvaluationRequest;
import eu.xenit.contentcloud.thunx.pdp.AuthenticationContext;
import eu.xenit.contentcloud.thunx.pdp.PolicyDecision;
import eu.xenit.contentcloud.thunx.pdp.PolicyDecisionPointClient;
import eu.xenit.contentcloud.thunx.pdp.PolicyDecisions;
import eu.xenit.contentcloud.thunx.pdp.RequestContext;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OpenPolicyAgentPDPClient implements PolicyDecisionPointClient {

    private final OpaClient opaClient;
    private OpaQueryProvider queryProvider;

    public OpenPolicyAgentPDPClient(OpaClient opaClient, OpaQueryProvider queryProvider) {
        Objects.requireNonNull(opaClient, "opaClient is required");
        Objects.requireNonNull(queryProvider, "queryProvider is required");

        this.opaClient = opaClient;
        this.queryProvider = queryProvider;
    }

    @Override
    public CompletableFuture<PolicyDecision> conditional(AuthenticationContext authContext, RequestContext requestContext) {
        // TODO how can we define 'unknowns' in a generic way ?
        // WARNING: do NOT list 'input' as unknown, or it will ignore the whole 'input' object itself
        // WARNING: do NOT list 'data' as unknown, or it will ignore the policy that is loaded in OPA itself
        var request = new PartialEvaluationRequest(
                this.queryProvider.createQuery(requestContext),
                createInput(authContext, requestContext),
                List.of("input.entity"));

        return opaClient.compile(request)
                .thenApply(response ->
                {
                    // list of possible partially evaluated queries from OPA
                    // we need to convert this to a single boolean expression
                    var opaQuerySet = response.getResult().getQueries();
                    var converter = new QuerySetToThunkExpressionConverter();
                    return converter.convert(opaQuerySet);
                })
                .thenApply(thunkExpression -> {
                    // if the expression can be resolved right now, there is no remaining predicate
                    if (thunkExpression.canBeResolved()) {
                        return thunkExpression.resolve() ? PolicyDecisions.allowed() : PolicyDecisions.denied();
                    } else {
                        // there is a remaining predicate
                        return PolicyDecisions.conditional(thunkExpression);
                    }
                });
    }

    static Map<String, Object> createInput(AuthenticationContext authContext, RequestContext requestContext) {
        return Map.of(
                "path", uriToPathArray(requestContext.getURI()),
                "method", requestContext.getHttpMethod(),
                "queryParams", requestContext.getQueryParams(),
                "auth", authContext,
                "user", authContext.getUser() // temp for backwards compat with existing policies
        );
    }

    static String[] uriToPathArray(URI uri) {
        Objects.requireNonNull(uri, "Argument 'uri' is required");
        uri = uri.normalize();

        var path = uri.getPath();
        if (path == null) {
            return new String[0];
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.length() == 0) {
            return new String[0];
        }

        return path.split("/");
    }


}
