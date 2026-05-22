package roomescape.auth;

import org.springframework.stereotype.Component;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class AdminInterceptor extends RoleCheckInterceptor {
    public AdminInterceptor(MemberService memberService) {
        super(memberService);
    }

    @Override
    protected boolean hasRequiredRole(Member member) {
        return member.isAdmin();
    }
}
