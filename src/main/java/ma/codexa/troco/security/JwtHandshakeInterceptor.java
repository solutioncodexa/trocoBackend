package ma.codexa.troco.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * JWT en query {@code ?access_token=} au handshake WebSocket (pattern MizanePro).
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("access_token");
        }
        if (!StringUtils.hasText(token)) {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("access_token=")) {
                        token = part.substring("access_token=".length());
                        break;
                    }
                }
            }
        }
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return false;
        }
        attributes.put("accessToken", token);
        attributes.put("email", jwtUtil.getEmailFromToken(token));
        attributes.put("fournisseurId", jwtUtil.getFournisseurIdFromToken(token));
        attributes.put("role", jwtUtil.getRoleFromToken(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
