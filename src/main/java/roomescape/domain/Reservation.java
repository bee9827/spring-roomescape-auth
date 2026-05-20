package roomescape.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import roomescape.common.exception.BusinessRuleViolationException;

public class Reservation {
    private final Long id;
    private final Member member;
    private final Theme theme;
    private final long version;
    private LocalDate date;
    private Time time;
    private ReservationStatus status;
    private LocalDateTime deletedAt;

    private Reservation(Long id, Member member, LocalDate date, Time time, Theme theme,
                        ReservationStatus status, LocalDateTime deletedAt, long version) {
        this.id = id;
        this.member = member;
        this.date = date;
        this.time = time;
        this.theme = theme;
        this.status = status;
        this.version = version;
        this.deletedAt = deletedAt;
    }

    public static Reservation createByUser(Member member, LocalDate date, Time time, Theme theme, LocalDateTime now) {
        if (time.isReservationBefore(now, date)) {
            throw new BusinessRuleViolationException("지난 시간에 대한 예약 생성은 불가능합니다.");
        }
        return new Reservation(null, member, date, time, theme, ReservationStatus.BOOKED, null, 0L);
    }

    public static Reservation createByAdmin(Member member, LocalDate date, Time time, Theme theme) {
        return new Reservation(null, member, date, time, theme, ReservationStatus.BOOKED, null, 0L);
    }

    public static Reservation reconstruct(Long id, Member member, LocalDate date, Time time, Theme theme,
                                          ReservationStatus status, LocalDateTime deletedAt, long version) {
        return new Reservation(id, member, date, time, theme, status, deletedAt, version);
    }

    public static Reservation reconstruct(Long id, Member member, LocalDate date, Time time, Theme theme) {
        return new Reservation(id, member, date, time, theme, ReservationStatus.BOOKED, null, 0L);
    }

    public void cancelByMember(Long memberId, LocalDateTime now) {
        if (!isOwnedBy(memberId)) {
            throw new BusinessRuleViolationException("본인의 예약만 취소할 수 있습니다.");
        }
        if (getTime().isReservationBefore(now, date)) {
            throw new BusinessRuleViolationException("지난 예약은 취소 불가능합니다.");
        }
        doCancel(now);
    }

    public void cancelByAdmin(LocalDateTime now) {
        doCancel(now);
    }

    private void doCancel(LocalDateTime now) {
        this.status = ReservationStatus.CANCELED;
        this.deletedAt = now;
    }

    public void update(LocalDate date, Time time) {
        this.date = date;
        this.time = time;
    }

    public boolean isActive() {
        return status == ReservationStatus.BOOKED;
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reservation that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getDate() {
        return date;
    }

    public Time getTime() {
        return time;
    }

    public Theme getTheme() {
        return theme;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
