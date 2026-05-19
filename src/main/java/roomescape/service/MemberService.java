package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.common.exception.BadRequestException;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.NotFoundException;
import roomescape.dao.MemberDao;
import roomescape.domain.Member;
import roomescape.domain.MemberRole;
import roomescape.dto.request.LoginRequestDto;
import roomescape.dto.request.SignupRequestDto;

@Service
public class MemberService {
    private final MemberDao memberDao;

    public MemberService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    public Member login(LoginRequestDto request) {
        Member member = memberDao.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.getPassword().equals(request.password())) {
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return member;
    }

    public Member signup(SignupRequestDto request) {
        if (memberDao.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        Member member = new Member(null, request.name(), request.email(), request.password(), MemberRole.USER);
        return memberDao.insert(member);
    }

    public Member findById(Long id) {
        return memberDao.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 멤버입니다."));
    }
}
