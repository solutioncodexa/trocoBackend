package ma.codexa.troco.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.service.UserDetailsServiceImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
                throw new IllegalArgumentException("WebSocket CONNECT sans JWT valide");
            }
            String email = jwtUtil.getEmailFromToken(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);
            String principalName = email;
            Long fournisseurId = null;
            if (user instanceof UserDetailsImpl udi) {
                if (udi.getId() != null) principalName = String.valueOf(udi.getId());
                fournisseurId = udi.getFournisseurId();
            }
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("fournisseurId", fournisseurId);
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principalName, null, user.getAuthorities());
            accessor.setUser(auth);
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            assertStoreTopic(accessor);
        }
        return message;
    }

    /**
     * Un client ne s'abonne qu'aux topics de sa boutique
     * ({@code /topic/store.{id}} et {@code /topic/store.{id}.revenue}).
     */
    private static void assertStoreTopic(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null || dest.isBlank()) {
            throw new IllegalArgumentException("Abonnement WebSocket sans destination");
        }
        if (dest.startsWith("/user/")) return;
        Map<String, Object> attrs = accessor.getSessionAttributes();
        Object fid = attrs != null ? attrs.get("fournisseurId") : null;
        if (!(fid instanceof Long id)) {
            throw new IllegalArgumentException("Abonnement WebSocket sans boutique");
        }
        String own = "/topic/store." + id;
        if (dest.equals(own) || dest.startsWith(own + ".")) return;
        log.warn("websocket_subscribe_denied dest={} store={}", dest, id);
        throw new IllegalArgumentException("Abonnement WebSocket refusé pour cette boutique");
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        String explicit = accessor.getFirstNativeHeader("access_token");
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.get("accessToken") instanceof String s) {
            return s;
        }
        return null;
    }
}
