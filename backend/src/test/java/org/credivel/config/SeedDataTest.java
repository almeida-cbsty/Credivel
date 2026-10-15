package org.credivel.config;

import org.credivel.news.NewsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seedtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SeedDataTest {
    @Autowired MockMvc mvc;
    @Autowired NewsRepository repository;
    @Autowired SeedData seed;

    @Test
    void servesBundledSiteImagesAndNews() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/imported/moz-1.jpg")).andExpect(status().isOk());
        mvc.perform(get("/api/news?size=50")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(55));
    }

    @Test
    void importsCompleteNewsAndDoesNotDuplicateOnRestart() throws Exception {
        assertThat(repository.count()).isEqualTo(55);
        var news = repository.findAll();
        assertThat(news).allSatisfy(item -> {
            assertThat(item.getTitle()).isNotBlank();
            assertThat(item.getSummary()).isNotBlank();
            assertThat(item.getContent()).isNotBlank();
            assertThat(item.getImageUrl()).startsWith("/imported/");
        });
        seed.run();
        assertThat(repository.count()).isEqualTo(55);
    }
}
