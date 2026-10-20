package ke.shulehub.identity.web;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import ke.shulehub.identity.Actor;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.Grant;
import ke.shulehub.identity.PersonRef;
import ke.shulehub.identity.RoleAssignments;
import ke.shulehub.identity.internal.ActorResolver;
import ke.shulehub.shared.tenancy.TenantContext;

/**
 * Runs after bearer-token authentication. Links the token to a person, binds the tenant from
 * {@code X-Tenant-Id} (refusing tenants the person has no role in) and sets the request's
 * {@link Actor}. Created inside the security config, not as a bean, so it runs exactly once
 * and in the right place.
 */
class TenantResolutionFilter extends OncePerRequestFilter {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final ActorResolver actorResolver;
    private final RoleAssignments roleAssignments;

    TenantResolutionFilter(ActorResolver actorResolver, RoleAssignments roleAssignments) {
        this.actorResolver = actorResolver;
        this.roleAssignments = roleAssignments;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            chain.doFilter(request, response);
            return;
        }
        Jwt jwt = token.getToken();
        boolean platformAdmin = ActorResolver.isPlatformAdmin(jwt);
        try {
            Optional<PersonRef> person = actorResolver.resolve(jwt);
            if (person.isEmpty() && !platformAdmin) {
                writeProblem(response, HttpServletResponse.SC_FORBIDDEN,
                        "Your login is not linked to a Shule Hub account yet. Ask your school to add your email or phone number.");
                return;
            }
            List<Grant> grants = List.of();
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                UUID tenantId;
                try {
                    tenantId = UUID.fromString(tenantHeader.trim());
                }
                catch (IllegalArgumentException ex) {
                    writeProblem(response, HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-Id must be a UUID");
                    return;
                }
                TenantContext.set(tenantId);
                if (person.isPresent()) {
                    grants = roleAssignments.grantsFor(person.get().id());
                }
                if (grants.isEmpty() && !platformAdmin) {
                    writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to this school");
                    return;
                }
            }
            ActorContext.set(new Actor(person.orElse(null), grants, platformAdmin));
            chain.doFilter(request, response);
        }
        finally {
            ActorContext.clear();
            TenantContext.clear();
        }
    }

    private static void writeProblem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        String title = status == HttpServletResponse.SC_FORBIDDEN ? "Forbidden" : "Bad Request";
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail.replace("\"", "'") + "\"}");
    }
}
