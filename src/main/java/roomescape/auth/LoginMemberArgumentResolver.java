package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.auth.session.SessionUtils;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.common.exception.UnauthenticatedException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final MemberService memberService;

    public LoginMemberArgumentResolver(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new UnauthenticatedException();
        }
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new UnauthenticatedException();
            }
            Long memberId = SessionUtils.parseMemberId(session.getAttribute("memberId"));
            if (memberId == null) {
                throw new UnauthenticatedException();
            }
            return memberService.findById(memberId);
        } catch (NumberFormatException | EntityNotFoundException e) {
            throw new UnauthenticatedException();
        }
    }
}
