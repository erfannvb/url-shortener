package nvb.dev.urlshortener.repository;

import nvb.dev.urlshortener.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByOriginalUrl(String originalUrl);

    Optional<ShortUrl> findByShortCode(String shortCode);

}
