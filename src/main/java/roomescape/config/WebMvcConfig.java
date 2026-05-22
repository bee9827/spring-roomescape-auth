package roomescape.config;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.AdminInterceptor;
import roomescape.auth.AuthFilter;
import roomescape.auth.LoginMemberArgumentResolver;
import roomescape.auth.ManagerInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AdminInterceptor adminInterceptor;
    private final ManagerInterceptor managerInterceptor;
    private final LoginMemberArgumentResolver loginMemberArgumentResolver;
    private final AuthFilter authFilter;

    public WebMvcConfig(
            AdminInterceptor adminInterceptor,
            ManagerInterceptor managerInterceptor,
            LoginMemberArgumentResolver loginMemberArgumentResolver,
            AuthFilter authFilter
    ) {
        this.adminInterceptor = adminInterceptor;
        this.managerInterceptor = managerInterceptor;
        this.loginMemberArgumentResolver = loginMemberArgumentResolver;
        this.authFilter = authFilter;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration() {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(authFilter);
        registration.addUrlPatterns("/reservations", "/reservations/*");
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
        registry.addInterceptor(managerInterceptor)
                .addPathPatterns("/manager/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMemberArgumentResolver);
    }
}
