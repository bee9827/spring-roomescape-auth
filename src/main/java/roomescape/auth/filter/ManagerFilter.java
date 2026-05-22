package roomescape.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@Component
public class ManagerFilter extends RoleCheckFilter {
    public ManagerFilter(MemberService memberService, ObjectMapper objectMapper) {
        super(memberService, objectMapper);
    }

    @Override
    protected boolean hasRequiredRole(Member member) {
        return member.isManager();
    }
}
