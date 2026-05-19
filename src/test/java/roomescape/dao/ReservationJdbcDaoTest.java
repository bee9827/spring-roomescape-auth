package roomescape.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import roomescape.dao.jdbc.MemberJdbcDao;
import roomescape.dao.jdbc.ReservationJdbcDao;
import roomescape.dao.jdbc.ThemeJdbcDao;
import roomescape.dao.jdbc.TimeJdbcDao;
import roomescape.domain.Member;
import roomescape.domain.MemberRole;
import roomescape.domain.Reservation;
import roomescape.domain.Theme;
import roomescape.domain.Time;
import roomescape.domain.vo.Name;

@JdbcTest
@Import({ReservationJdbcDao.class, TimeJdbcDao.class, ThemeJdbcDao.class, MemberJdbcDao.class})
@ActiveProfiles("test")
class ReservationJdbcDaoTest {

    @Autowired
    private ReservationDao reservationDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private TimeDao timeDao;
    @Autowired
    private ThemeDao themeDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member member;
    private Time time;
    private Theme theme;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "INSERT INTO members(name, email, password, role) VALUES (?, ?, ?, ?)",
                "유저", "user@test.com", "password", "USER"
        );
        member = memberDao.findByEmail("user@test.com").orElseThrow();
        time = timeDao.insert(new Time(LocalTime.of(13, 0)));
        theme = themeDao.insert(new Theme(new Name("방탈출"), "http://url", "설명"));
    }

    @Nested
    class FindAll {

        @Test
        @DisplayName("전체 예약 목록을 조회한다")
        void findAll() {
            Reservation r1 = reservationDao.insert(new Reservation(member, LocalDate.of(2026, 6, 1), time, theme));
            Reservation r2 = reservationDao.insert(new Reservation(member, LocalDate.of(2026, 6, 2), time, theme));

            List<Reservation> result = reservationDao.findAll();

            assertThat(result).hasSize(2).containsExactlyInAnyOrder(r1, r2);
        }
    }

    @Nested
    class Update {

        @Test
        @DisplayName("정상적으로 예약을 수정하면 최신 상태를 반환한다")
        void updatesReservation() {
            Reservation saved = reservationDao.insert(
                    new Reservation(member, LocalDate.of(2026, 6, 1), time, theme));

            saved.update(LocalDate.of(2026, 6, 2), time);
            Reservation updated = reservationDao.update(saved);

            assertThat(updated.getDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        }
    }
}
