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
    public static final String LOGIN_MEMBER_ATTRIBUTE = "loginMember";

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
            if (memberId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            Member member = memberService.findById(memberId);
            if (!member.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            request.setAttribute(LOGIN_MEMBER_ATTRIBUTE, member);
            return true;
        } catch (NumberFormatException | ClassCastException | EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
