package ma.codexa.goldyara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan("ma.codexa.goldyara.config")
@ComponentScan(basePackages = "ma.codexa.goldyara")
@EnableAsync
public class GoldyaraApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoldyaraApplication.class, args);
	}

}
