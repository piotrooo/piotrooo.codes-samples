package codes.piotrooo.dependencyreview;

import codes.piotrooo.dependencyreview.cli.ReviewRunner;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DependencyReviewApplication {

    static void main(String[] args) {
        SpringApplication.run(DependencyReviewApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(ReviewRunner reviewRunner) {
        return reviewRunner::run;
    }
}
