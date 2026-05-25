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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import roomescape.auth.session.SessionUtils;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    public AuthFilter(MemberService memberService, ObjectMapper objectMapper) {
        this.memberService = memberService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("memberId") == null) {
            sendUnauthorized(request, response);
            return;
        }
        try {
            Long memberId = SessionUtils.parseMemberId(session.getAttribute("memberId"));
            if (memberId == null) {
                sendUnauthorized(request, response);
                return;
            }
            Member member = memberService.findById(memberId);
            request.setAttribute(SessionUtils.LOGIN_MEMBER_ATTRIBUTE, member);
        } catch (EntityNotFoundException e) {
            sendUnauthorized(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
