package nvb.dev.urlshortener.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String originalUrl;

    @Column(unique = true)
    private String shortCode;

    private LocalDateTime createdAt;

    @PrePersist
    public void initializeCreatedAt() {
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
    }

}
