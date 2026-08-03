package nvb.dev.urlshortener.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping(path = "/health")
    public String getHealth() {
        return "Service is running";
    }

}
