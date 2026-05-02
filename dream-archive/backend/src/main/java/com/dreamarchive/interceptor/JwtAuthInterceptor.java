package com.dreamarchive.interceptor;

import com.dreamarchive.common.Result;
import com.dreamarchive.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * JWT 鉴权拦截器：校验请求头中的 Bearer Token，将当前用户 ID、用户名、角色写入 request 属性。
 * 公开接口（登录、注册、公开梦境列表等）放行；其余接口需有效令牌。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/dream/public",
            "/dream/hot",
            "/category/list",
            "/category/list/count"
    );

    private static final Pattern CATEGORY_ID_PATH = Pattern.compile("^/category/\\d+$");
    private static final Pattern DREAM_ID_PATH = Pattern.compile("^/dream/\\d+$");
    private static final Pattern USER_ID_PATH = Pattern.compile("^/user/\\d+$");
    private static final Pattern DREAM_USER_PATH = Pattern.compile("^/dream/user/\\d+$");
    private static final Pattern COMMENT_DREAM_PATH = Pattern.compile("^/comment/dream/\\d+$");
    private static final Pattern COMMENT_USER_PATH = Pattern.compile("^/comment/user/\\d+$");

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (isPublicEndpoint(request)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "未登录或登录已过期");
            return false;
        }

        Claims claims = jwtUtil.parseToken(token);
        request.setAttribute("currentUserId", claims.get("userId", Long.class));
        request.setAttribute("currentUsername", claims.getSubject());
        request.setAttribute("currentUserRole", claims.get("role", String.class));
        return true;
    }

    /** 从 Authorization: Bearer 或自定义 token 头解析 JWT。 */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return request.getHeader("token");
    }

    /** 判断是否为无需登录的路径（注册登录 POST、公开 GET、静态上传等）。 */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String path = resolveApiPath(request);

        if (HttpMethod.POST.matches(method)) {
            return "/user/login".equals(path) || "/user/register".equals(path);
        }

        if (HttpMethod.GET.matches(method)) {
            if (PUBLIC_GET_PATHS.contains(path)) {
                return true;
            }
            return CATEGORY_ID_PATH.matcher(path).matches()
                    || DREAM_ID_PATH.matcher(path).matches()
                    || USER_ID_PATH.matcher(path).matches()
                    || DREAM_USER_PATH.matcher(path).matches()
                    || COMMENT_DREAM_PATH.matcher(path).matches()
                    || COMMENT_USER_PATH.matcher(path).matches();
        }

        return "/error".equals(path) || path.startsWith("/uploads/");
    }

    /** 统一得到去掉 context-path 与 /api 前缀后的路径，便于与白名单匹配。 */
    private String resolveApiPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            return normalizePath(path);
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            String stripped = requestUri.substring(contextPath.length());
            return normalizePath(stripped.isBlank() ? "/" : stripped);
        }
        return normalizePath(requestUri);
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }

        String path = rawPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        if ("/api".equals(path)) {
            return "/";
        }
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        }
        return path;
    }

    /** 以 JSON 形式返回 401，供前端统一处理。 */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> result = Result.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
