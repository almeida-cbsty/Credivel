package org.credivel.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.credivel.news.News;
import org.credivel.news.NewsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements CommandLineRunner {
    private final NewsRepository repository;
    private final ObjectMapper mapper;

    public SeedData(NewsRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Existing installations keep their data. Import only into an empty database.
        if (repository.count() > 0) return;
        try (var stream = new ClassPathResource("seed/news.json").getInputStream()) {
            for (var row : mapper.readTree(stream)) {
                // IDs belong to the source database; generate fresh ones on import.
                ((ObjectNode) row).remove("id");
                repository.save(mapper.treeToValue(row, News.class));
            }
        }
    }
}
