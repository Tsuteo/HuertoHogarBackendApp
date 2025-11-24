package com.huertohogar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.huertohogar.backend.model.Producto;
import com.huertohogar.backend.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {


    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);

    }

    @Bean
    public CommandLineRunner initData(ProductoRepository repository) {
        return (args) -> {
            repository.save(new Producto("Manzana Backend", 1000, 50, "Frutas"));
            repository.save(new Producto("Miel del Servidor", 5000, 20, "Orgánico"));
            System.out.println("✅ Datos de prueba cargados en H2");
        };
    }
}
