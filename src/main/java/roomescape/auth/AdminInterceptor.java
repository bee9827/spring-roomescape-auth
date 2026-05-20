package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
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
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("memberId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        try {
            Long memberId = parseMemberId(session.getAttribute("memberId"));
            if (memberId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            Member member = memberService.findById(memberId);
            if (!member.isAdmin()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            request.setAttribute(LOGIN_MEMBER_ATTRIBUTE, member);
            return true;
        } catch (NumberFormatException | ClassCastException | EntityNotFoundException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
