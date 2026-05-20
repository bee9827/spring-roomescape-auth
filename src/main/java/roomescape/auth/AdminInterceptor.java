package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.common.exception.NotFoundException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    public static final String LOGIN_MEMBER_ATTRIBUTE = "loginMember";

    private final MemberService memberService;

    public AdminInterceptor(MemberService memberService) {
        this.memberService = memberService;
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
            Long memberId = (Long) session.getAttribute("memberId");
            Member member = memberService.findById(memberId);
            if (!member.isAdmin()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            request.setAttribute(LOGIN_MEMBER_ATTRIBUTE, member);
            return true;
        } catch (ClassCastException | NotFoundException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
