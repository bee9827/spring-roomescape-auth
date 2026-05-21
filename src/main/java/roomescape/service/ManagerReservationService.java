package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.EntityNotFoundException;
import roomescape.common.exception.UnauthorizedException;
import roomescape.dao.ReservationDao;
import roomescape.dao.TimeDao;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Time;
import roomescape.dto.request.ReservationPatchDto;

@Service
@Transactional(readOnly = true)
public class ManagerReservationService {
    private final ReservationDao reservationDao;
    private final TimeDao timeDao;

    public ManagerReservationService(ReservationDao reservationDao, TimeDao timeDao) {
        this.reservationDao = reservationDao;
        this.timeDao = timeDao;
    }

    public List<Reservation> findAllByStore(Member manager) {
        return reservationDao.findAllByStoreId(manager.getStoreId());
    }

    @Transactional
    public Reservation update(Long id, Member manager, ReservationPatchDto request) {
        Reservation reservation = findByIdForManager(id, manager);
        Time time = timeDao.findById(request.timeId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 시간입니다."));
        reservation.update(request.date(), time);
        return reservationDao.update(reservation);
    }

    @Transactional
    public void cancel(Long id, Member manager) {
        Reservation reservation = findByIdForManager(id, manager);
        reservation.cancelByAdmin(LocalDateTime.now());
        reservationDao.update(reservation);
    }

    private Reservation findByIdForManager(Long id, Member manager) {
        Reservation reservation = reservationDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 예약입니다."));
        if (!manager.getStoreId().equals(reservation.getStoreId())) {
            throw new UnauthorizedException();
        }
        return reservation;
    }
}
