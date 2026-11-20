package com.moldelab.shared;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.moldelab.modules.export.PdfPatternRenderer;
import com.moldelab.modules.pattern.domain.GarmentCatalog;
import com.moldelab.modules.pattern.domain.PatternEngine;
import com.moldelab.modules.pattern.geometry.Vec2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Clock;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public AppConfig(@Value("${moldelab.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public GarmentCatalog garmentCatalog() {
        return new GarmentCatalog();
    }

    @Bean
    public PatternEngine patternEngine(GarmentCatalog catalog) {
        return new PatternEngine(catalog);
    }

    @Bean
    public PdfPatternRenderer pdfPatternRenderer() {
        return new PdfPatternRenderer();
    }

    /** Vec2 vira [x, y] com 2 casas decimais — JSON compacto para o front. */
    @Bean
    public Module vec2Module() {
        SimpleModule m = new SimpleModule("vec2");
        m.addSerializer(Vec2.class, new JsonSerializer<>() {
            @Override
            public void serialize(Vec2 v, JsonGenerator gen, SerializerProvider sp) throws IOException {
                gen.writeStartArray();
                gen.writeNumber(Math.round(v.x() * 100) / 100.0);
                gen.writeNumber(Math.round(v.y() * 100) / 100.0);
                gen.writeEndArray();
            }
        });
        return m;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
