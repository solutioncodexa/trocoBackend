package ma.codexa.troco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan("ma.codexa.troco.config")
@ComponentScan(basePackages = "ma.codexa.troco")
@EnableAsync
@EnableScheduling
public class TrocoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrocoApplication.class, args);
	}

}
