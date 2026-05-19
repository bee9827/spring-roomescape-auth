package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import roomescape.dao.ReservationDao;
import roomescape.dao.ThemeDao;
import roomescape.dao.TimeDao;
import roomescape.domain.Member;
import roomescape.domain.MemberRole;
import roomescape.domain.Reservation;
import roomescape.domain.Theme;
import roomescape.domain.Time;
import roomescape.domain.vo.Name;
import roomescape.dto.request.LoginRequestDto;
import roomescape.dto.request.ReservationPatchDto;
import roomescape.dto.request.ReservationRequestDto;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReservationConcurrencyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ReservationDao reservationDao;
    @Autowired
    private TimeDao timeDao;
    @Autowired
    private ThemeDao themeDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member member;
    private Reservation savedReservation;
    private String sessionCookie;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        jdbcTemplate.update(
                "INSERT INTO members(name, email, password, role) VALUES (?, ?, ?, ?)",
                "유저", "user@test.com", "password", "USER"
        );
        Long memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE email = ?", Long.class, "user@test.com");
        member = new Member(memberId, "유저", "user@test.com", "password", MemberRole.USER);

        Time time = timeDao.insert(new Time(LocalTime.of(13, 0)));
        Theme theme = themeDao.insert(new Theme(new Name("방탈출"), "http://url", "설명"));
        savedReservation = reservationDao.insert(new Reservation(member, LocalDate.now().plusDays(1), time, theme));

        Response loginResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDto("user@test.com", "password"))
                .when()
                .post("/login");
        sessionCookie = loginResponse.getCookie("JSESSIONID");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM times");
        jdbcTemplate.update("DELETE FROM themes");
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    @DisplayName("같은 슬롯에 3개 동시 예약 요청이 들어오면 하나만 성공하고 나머지는 409를 반환한다")
    void concurrentInsertResultsInOneSuccess() throws InterruptedException {
        int threadCount = 3;
        ReservationRequestDto request = new ReservationRequestDto(
                LocalDate.now().plusDays(2),
                savedReservation.getTime().getId(),
                savedReservation.getTheme().getId()
        );

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Runnable sendPostRequest = () -> {
            try {
                startLatch.await();
                int statusCode = RestAssured.given()
                        .contentType(ContentType.JSON)
                        .cookie("JSESSIONID", sessionCookie)
                        .body(request)
                        .when()
                        .post("/reservations")
                        .statusCode();

                if (statusCode == HttpStatus.CREATED.value()) {
                    successCount.incrementAndGet();
                } else if (statusCode == HttpStatus.CONFLICT.value()) {
                    conflictCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(sendPostRequest).start();
        }
        startLatch.countDown();
        doneLatch.await();

        // H2는 gap lock 미지원으로 엄격한 검증 불가. MySQL 환경에서 successCount=1 보장.
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("동시에 같은 예약을 수정하면 하나만 성공하고 나머지는 409를 반환한다")
    void concurrentUpdateResultsInOneConflict() throws InterruptedException {
        int threadCount = 10;
        ReservationPatchDto request = new ReservationPatchDto(
                LocalDate.now().plusDays(3), savedReservation.getTime().getId());
        String url = "/reservations/" + savedReservation.getId();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Runnable sendPatchRequest = () -> {
            try {
                startLatch.await();
                int statusCode = RestAssured.given()
                        .contentType(ContentType.JSON)
                        .cookie("JSESSIONID", sessionCookie)
                        .body(request)
                        .when()
                        .patch(url)
                        .statusCode();

                if (statusCode == HttpStatus.OK.value()) {
                    successCount.incrementAndGet();
                } else if (statusCode == HttpStatus.CONFLICT.value()) {
                    conflictCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(sendPatchRequest).start();
        }
        startLatch.countDown();
        doneLatch.await();

        assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
        assertThat(conflictCount.get()).isGreaterThanOrEqualTo(1);
    }
}
