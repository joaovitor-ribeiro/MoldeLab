package com.moldelab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Frontend servido pelo backend (imagem Docker): rotas do React caem no index.html; API não. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:moldelab-spa;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "moldelab.ai.enabled=false"
})
class SpaFallbackTest {

    @Autowired MockMvc mvc;

    @Test
    void rotasDoFrontendDevolvemIndexHtml() throws Exception {
        for (String path : new String[]{"/medidas", "/editor", "/projetos/abc"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("moldelab-spa")));
        }
    }

    @Test
    void arquivosEstaticosSaoServidos() throws Exception {
        mvc.perform(get("/assets/app.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("console.log")));
        mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
    }

    @Test
    void rotaDeApiInexistenteContinua404EmJson() throws Exception {
        mvc.perform(get("/api/nao-existe")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/catalog")).andExpect(status().isOk());
    }
}
