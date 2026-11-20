package com.moldelab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:moldelab-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "moldelab.ai.enabled=false"
})
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    static final String MEASUREMENTS = """
            {"height":165,"bust":92,"waist":74,"hip":100,"neck":35,"shoulderWidth":38.5,"backWaistLength":41,
             "armLength":59,"upperArm":28.5,"wrist":15.8,"waistToHip":20.5,"waistToKnee":58.8,"waistToFloor":104.8,
             "thigh":56,"crotchDepth":27}""";

    @Test
    void catalogoListaModelosComParametros() throws Exception {
        mvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].type", hasItems("SAIA_RETA", "SAIA_GODE", "CAMISETA")))
                .andExpect(jsonPath("$[0].params[0].key").exists());
    }

    @Test
    void guiaEtabelasDeMedidas() throws Exception {
        mvc.perform(get("/api/measurements/guide")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='BACK_WAIST_LENGTH')].jsonName", contains("backWaistLength")));
        mvc.perform(get("/api/measurements/standard-sizes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurements.bust").isNumber());
    }

    @Test
    void draftDevolvePecasComPontosCompactos() throws Exception {
        mvc.perform(post("/api/patterns/draft").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garmentType\":\"CAMISETA\",\"measurements\":" + MEASUREMENTS + ",\"params\":{\"sleeveLength\":25}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pieces", hasSize(4)))
                .andExpect(jsonPath("$.pieces[0].code").value("A"))
                .andExpect(jsonPath("$.pieces[0].cutLine[0]", hasSize(2)))
                .andExpect(jsonPath("$.params.sleeveLength").value(25.0))
                .andExpect(jsonPath("$.fabric[0].lengthMm").isNumber());
    }

    @Test
    void draftSemMedidasDevolve422ComLista() throws Exception {
        mvc.perform(post("/api/patterns/draft").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garmentType\":\"SAIA_RETA\",\"measurements\":{\"waist\":70}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_MEASUREMENTS"))
                .andExpect(jsonPath("$.details.missing", containsInAnyOrder("hip", "waistToHip")));
    }

    @Test
    void medidaNegativaEhErroDeValidacao() throws Exception {
        mvc.perform(post("/api/patterns/draft").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"garmentType\":\"SAIA_GODE\",\"measurements\":{\"waist\":-70}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void pdfEhGerado() throws Exception {
        byte[] body = mvc.perform(post("/api/patterns/pdf").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draft\":{\"garmentType\":\"SAIA_RETA\",\"measurements\":" + MEASUREMENTS + "},"
                                + "\"paper\":\"A4\",\"title\":\"Saia da Ana\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("saia-da-ana.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(new String(body, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void perfilDeMedidasCrudComEdicao() throws Exception {
        String created = mvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"measurements\":" + MEASUREMENTS + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(created).get("id").asText();

        mvc.perform(put("/api/profiles/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana (atualizado)\",\"measurements\":{\"waist\":72.5,\"hip\":99}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana (atualizado)"))
                .andExpect(jsonPath("$.measurements.waist").value(72.5))
                .andExpect(jsonPath("$.measurements.bust").doesNotExist());

        mvc.perform(get("/api/profiles/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements.hip").value(99.0));
        mvc.perform(delete("/api/profiles/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/profiles/" + id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void projetoSalvaEAtualiza() throws Exception {
        String body = "{\"name\":\"Minha saia\",\"garmentType\":\"SAIA_GODE\",\"measurements\":" + MEASUREMENTS
                + ",\"params\":{\"fraction\":0.5},\"seams\":{\"seamCm\":1.5},\"sketchDataUrl\":\"data:image/png;base64,iVBORw0KGgo=\"}";
        String created = mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(created);
        mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$[0].hasSketch").value(true));
        mvc.perform(get("/api/projects/" + node.get("id").asText())).andExpect(status().isOk())
                .andExpect(jsonPath("$.params.fraction").value(0.5))
                .andExpect(jsonPath("$.seams.seamCm").value(1.5));
    }

    @Test
    void projetoComPerfilInexistenteDevolve409() throws Exception {
        String body = "{\"name\":\"x\",\"garmentType\":\"SAIA_GODE\",\"profileId\":\"00000000-0000-0000-0000-000000000001\",\"measurements\":{}}";
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void errosPadraoDoSpringNaoViram500() throws Exception {
        mvc.perform(get("/api/nao-existe")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/profiles/nao-e-uuid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/patterns/draft").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(delete("/api/catalog")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void iaDesabilitadaDevolve503() throws Exception {
        mvc.perform(get("/api/ai/status")).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(multipart("/api/ai/analyze-garment").file(new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_DISABLED"));
    }
}
