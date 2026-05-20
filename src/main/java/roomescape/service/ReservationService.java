package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import roomescape.common.exception.BadRequestException;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.NotFoundException;
import roomescape.dao.ReservationDao;
import roomescape.dao.ThemeDao;
import roomescape.dao.TimeDao;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Theme;
import roomescape.domain.Time;
import roomescape.dto.request.ReservationPatchDto;
import roomescape.dto.request.ReservationRequestDto;

@Service
@Transactional(readOnly = true)
public class ReservationService {
    private final ReservationDao reservationDao;
    private final TimeDao timeDao;
    private final ThemeDao themeDao;

    public ReservationService(ReservationDao reservationDao, TimeDao timeDao, ThemeDao themeDao) {
        this.reservationDao = reservationDao;
        this.timeDao = timeDao;
        this.themeDao = themeDao;
    }

    public List<Reservation> findAllByMemberId(Long memberId) {
        return reservationDao.findAllByMemberId(memberId);
    }

    public Reservation findActiveById(Long id) {
        Reservation reservation = reservationDao.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
        if (!reservation.isActive()) {
            throw new NotFoundException("존재하지 않는 예약입니다.");
        }
        return reservation;
    }

    @Transactional
    public Reservation create(Member member, ReservationRequestDto request) {
        Reservation reservation = buildReservation(member, request);
        reservation.validateCreate(LocalDateTime.now());
        try {
            return reservationDao.insert(reservation);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("이미 존재하는 예약이 있습니다.");
        }
    }

    @Transactional
    public Reservation updateByUser(Long id, Long memberId, ReservationPatchDto request) {
        Reservation reservation = findActiveById(id);
        if (!reservation.isOwnedBy(memberId)) {
            throw new BadRequestException("본인의 예약만 수정할 수 있습니다.");
        }
        Time time = timeDao.findById(request.timeId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 시간입니다."));
        reservation.update(request.date(), time);
        return reservationDao.update(reservation);
    }

    @Transactional
    public void cancel(Long id, Long memberId) {
        Reservation reservation = reservationDao.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
        if (!reservation.isOwnedBy(memberId)) {
            throw new BadRequestException("본인의 예약만 취소할 수 있습니다.");
        }
        reservation.cancelIfValid(LocalDateTime.now());
        reservationDao.update(reservation);
    }

    private Reservation buildReservation(Member member, ReservationRequestDto request) {
        Time time = timeDao.findById(request.timeId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 시간입니다."));
        Theme theme = themeDao.findById(request.themeId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 테마입니다."));
        if (reservationDao.selectForUpdateByThemeIdAndTimeIdAndDate(request.themeId(), request.timeId(), request.date())) {
            throw new ConflictException("이미 존재하는 예약이 있습니다.");
        }
        return new Reservation(member, request.date(), time, theme);
    }
}
