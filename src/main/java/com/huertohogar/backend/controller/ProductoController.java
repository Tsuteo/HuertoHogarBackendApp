package com.huertohogar.backend.controller;

import com.huertohogar.backend.model.Producto;
import com.huertohogar.backend.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository repository;


    @GetMapping
    public List<Producto> obtenerTodos() {
        return repository.findAll();
    }

    @PostMapping
    public Producto guardarProducto(@RequestBody Producto producto) {
        return repository.save(producto);
    }
}