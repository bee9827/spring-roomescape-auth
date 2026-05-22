package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.common.exception.UnauthenticatedException;
import roomescape.common.exception.UnauthorizedException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

public abstract class RoleCheckInterceptor implements HandlerInterceptor {
    protected final MemberService memberService;

    protected RoleCheckInterceptor(MemberService memberService) {
        this.memberService = memberService;
    }

    protected abstract boolean hasRequiredRole(Member member);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("memberId") == null) {
            throw new UnauthenticatedException();
        }
        try {
            Long memberId = SessionUtils.parseMemberId(session.getAttribute("memberId"));
            Member member = memberService.findById(memberId);
            if (!hasRequiredRole(member)) {
                throw new UnauthorizedException();
            }
            return true;
        } catch (NumberFormatException | EntityNotFoundException e) {
            throw new UnauthenticatedException();
        }
    }
}
