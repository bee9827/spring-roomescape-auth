package roomescape.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import roomescape.common.exception.BusinessRuleViolationException;
import roomescape.common.exception.EntityNotFoundException;

public class Reservation {
    private final Long id;
    private final Member member;
    private final Theme theme;
    private final long version;
    private LocalDate date;
    private Time time;
    private ReservationStatus status;
    private LocalDateTime deletedAt;

    private Reservation(Builder builder) {
        this.id = builder.id;
        this.member = builder.member;
        this.date = builder.date;
        this.time = builder.time;
        this.theme = builder.theme;
        this.status = builder.status;
        this.deletedAt = builder.deletedAt;
        this.version = builder.version;
    }

    public static Reservation createByUser(Member member, LocalDate date, Time time, Theme theme, LocalDateTime now) {
        if (time.isReservationBefore(now, date)) {
            throw new BusinessRuleViolationException("지난 시간에 대한 예약 생성은 불가능합니다.");
        }
        return new Builder()
                .member(member).date(date).time(time).theme(theme)
                .build();
    }

    public static Reservation createByAdmin(Member member, LocalDate date, Time time, Theme theme) {
        return new Builder()
                .member(member).date(date).time(time).theme(theme)
                .build();
    }

    public static Reservation reconstruct(Long id, Member member, LocalDate date, Time time, Theme theme,
                                          ReservationStatus status, LocalDateTime deletedAt, long version) {
        return new Builder()
                .id(id).member(member).date(date).time(time).theme(theme)
                .status(status).deletedAt(deletedAt).version(version)
                .build();
    }

    public static Reservation reconstruct(Long id, Member member, LocalDate date, Time time, Theme theme) {
        return reconstruct(id, member, date, time, theme, ReservationStatus.BOOKED, null, 0L);
    }

    public void cancelByMember(Long memberId, LocalDateTime now) {
        if (!isOwnedBy(memberId)) {
            throw new EntityNotFoundException("존재하지 않는 예약입니다.");
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

    public void updateByMember(Long memberId, LocalDate date, Time time) {
        if (!isOwnedBy(memberId)) {
            throw new EntityNotFoundException("존재하지 않는 예약입니다.");
        }
        this.date = date;
        this.time = time;
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
    public boolean equals(Object o) {
        if (!(o instanceof Reservation that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public LocalDate getDate() { return date; }
    public Time getTime() { return time; }
    public Theme getTheme() { return theme; }
    public ReservationStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    private static class Builder {
        private Long id;
        private Member member;
        private LocalDate date;
        private Time time;
        private Theme theme;
        private ReservationStatus status = ReservationStatus.BOOKED;
        private LocalDateTime deletedAt;
        private long version = 0L;

        Builder id(Long id) {
            this.id = id;
            return this;
        }

        Builder member(Member member) {
            this.member = member;
            return this;
        }

        Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        Builder time(Time time) {
            this.time = time;
            return this;
        }

        Builder theme(Theme theme) {
            this.theme = theme;
            return this;
        }

        Builder status(ReservationStatus status) {
            this.status = status;
            return this;
        }

        Builder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        Builder version(long version) {
            this.version = version;
            return this;
        }

        Reservation build() {
            return new Reservation(this);
        }
    }
}
