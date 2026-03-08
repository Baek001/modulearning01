package kr.or.ddit.comm.conf;

import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SiteMeshFilterConfig {
	@Bean
	public FilterRegistrationBean<ConfigurableSiteMeshFilter> sitemeshFilter(){
		ConfigurableSiteMeshFilter filter = ConfigurableSiteMeshFilter.create(builder -> 
			builder.setDecoratorPrefix("/WEB-INF/decorators/")
					.addDecoratorPath("/**", "mazer-layout.jsp")
					.addExcludedPath("/ajax/**")
					.addExcludedPath("/login")
		);
		FilterRegistrationBean<ConfigurableSiteMeshFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.setOrder(Ordered.LOWEST_PRECEDENCE - 200);
		registration.addUrlPatterns("/*");
		return registration;
	}
}

