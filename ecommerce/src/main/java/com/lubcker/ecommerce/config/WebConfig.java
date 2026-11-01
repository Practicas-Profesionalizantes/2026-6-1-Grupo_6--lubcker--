package com.lubcker.ecommerce.config;

import com.lubcker.ecommerce.controller.ArchivoController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Sirve las imagenes subidas: GET /uploads/<archivo>
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(ArchivoController.CARPETA.toUri().toString());
    }
}
