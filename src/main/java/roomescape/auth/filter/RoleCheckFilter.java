package roomescape.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import roomescape.auth.session.SessionUtils;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.common.exception.UnauthenticatedException;
import roomescape.common.exception.UnauthorizedException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

public abstract class RoleCheckFilter extends OncePerRequestFilter {
    protected final MemberService memberService;
    private final ObjectMapper objectMapper;

    protected RoleCheckFilter(MemberService memberService, ObjectMapper objectMapper) {
        this.memberService = memberService;
        this.objectMapper = objectMapper;
    }

    protected abstract boolean hasRequiredRole(Member member);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("memberId") == null) {
            sendError(request, response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return;
        }
        try {
            Long memberId = SessionUtils.parseMemberId(session.getAttribute("memberId"));
            Member member = memberService.findById(memberId);
            if (!hasRequiredRole(member)) {
                sendError(request, response, HttpStatus.FORBIDDEN, "권한이 없습니다.");
                return;
            }
        } catch (NumberFormatException | EntityNotFoundException e) {
            sendError(request, response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response,
                           HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
