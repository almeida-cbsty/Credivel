package org.credivel.news; import java.util.Optional; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface NewsRepository extends JpaRepository<News,Long>{Page<News> findByStatus(NewsStatus s,Pageable p);Optional<News> findBySlugAndStatus(String slug,NewsStatus s);}
