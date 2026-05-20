package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import roomescape.common.exception.NotFoundException;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final MemberService memberService;

    public LoginMemberArgumentResolver(MemberService memberService) {
        this.memberService = memberService;
    }

    private Long parseMemberId(Object raw) {
        if (raw instanceof Long l) return l;
        if (raw instanceof String s) return Long.parseLong(s);
        return null;
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Object cached = request.getAttribute(AdminInterceptor.LOGIN_MEMBER_ATTRIBUTE);
        if (cached instanceof Member member) {
            return member;
        }

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            Long memberId = parseMemberId(session.getAttribute("memberId"));
            if (memberId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
            return memberService.findById(memberId);
        } catch (NumberFormatException | ClassCastException | NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
