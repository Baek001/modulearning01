package kr.or.ddit.comm.conf;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@ConfigurationProperties(prefix = "cors")
@Data
@Configuration
public class CorsJavaConfig {
	private List<String> allowOrigins;
	private List<String> allowMethods;
	private List<String> allowHeaders;
	
	private String restUri;
	
	@Bean
	public CorsConfigurationSource corsConfigSource() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.setAllowedOrigins(allowOrigins);
		corsConfig.setAllowedMethods(allowMethods);
		corsConfig.setAllowedHeaders(allowHeaders);
		corsConfig.setAllowCredentials(true);
		
		UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
		configSource.registerCorsConfiguration(restUri, corsConfig);
		
		return configSource;
	}
	
//	@Bean // SecurityFilteChain 내의 필터로 이동.
	public FilterRegistrationBean<CorsFilter> corsFilter(){
		CorsFilter filter = new CorsFilter(corsConfigSource());
		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<CorsFilter>();
		registration.setFilter(filter);
		registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
		registration.addUrlPatterns("/*");
		return registration;
	}
	
	@PostConstruct
	public void init() {
		log.info("cors.allow-origins : {}", allowOrigins);
		log.info("cors.allow-methods : {}", allowMethods);
		log.info("cors.allow-headers : {}", allowHeaders);
	}
}













