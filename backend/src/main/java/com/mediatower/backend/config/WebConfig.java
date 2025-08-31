// Chemin : src/main/java/com/mediatower/backend/config/WebConfig.java

package com.mediatower.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORRECTION : On rend la règle plus spécifique pour ne cibler que votre API.
        // Cela empêche d'exposer accidentellement d'autres endpoints de Spring.
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5174") // Le port de votre frontend est bien autorisé
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cette configuration est conservée telle quelle.
        // Elle expose le dossier 'uploads' afin qu'il soit accessible via l'URL.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}