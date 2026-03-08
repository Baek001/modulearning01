package kr.or.ddit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StarworksGroupwareApplication {

	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		SpringApplication.run(StarworksGroupwareApplication.class, args);
	}

//	@Value("${file-info.board-path}")
//	private String dummy;
//
//	@PostConstruct
//	public void init() {
//		System.out.println(dummy);
//	}

}
