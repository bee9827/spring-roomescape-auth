package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class AuthFilterTest {

    private AuthFilter authFilter;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter(new ObjectMapper());
    }

    @Test
    @DisplayName("세션이 없으면 401을 반환한다")
    void returns401WhenNoSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reservations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/problem+json");
    }

    @Test
    @DisplayName("세션에 memberId가 없으면 401을 반환한다")
    void returns401WhenNoMemberId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reservations");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("세션에 memberId가 있으면 다음 필터로 넘어간다")
    void continuesFilterChainWhenAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", 1L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reservations");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean[] chainCalled = {false};
        FilterChain filterChain = (req, res) -> chainCalled[0] = true;

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(chainCalled[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
