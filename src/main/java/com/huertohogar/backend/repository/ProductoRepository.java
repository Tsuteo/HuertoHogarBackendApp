package com.huertohogar.backend.repository;

import com.huertohogar.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface ProductoRepository extends JpaRepository<Producto, Long>{
}
