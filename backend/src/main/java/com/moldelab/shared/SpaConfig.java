package com.moldelab.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Em produção o build do frontend vai para {@code classpath:/static} (ver Dockerfile) e o backend serve
 * tudo na mesma origem. Rotas do React Router (/medidas, /editor...) caem no index.html; /api e /actuator
 * nunca — rota de API inexistente continua 404 em JSON. Em desenvolvimento não há /static e nada muda.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        if (path.startsWith("api/") || path.equals("api") || path.startsWith("actuator")) return null;
                        Resource asked = location.createRelative(path);
                        if (asked.exists() && asked.isReadable()) return asked;
                        if (path.contains(".")) return null; // arquivo inexistente (ex.: /assets/x.js) → 404
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
