package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final MemberService memberService;

    public AdminInterceptor(MemberService memberService) {
        this.memberService = memberService;
    }

    private Long parseMemberId(Object raw) {
        if (raw instanceof Long l) return l;
        if (raw instanceof String s) return Long.parseLong(s);
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("memberId") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            Long memberId = parseMemberId(session.getAttribute("memberId"));
            Member member = memberService.findById(memberId);
            if (!member.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return true;
        } catch (NumberFormatException | EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
