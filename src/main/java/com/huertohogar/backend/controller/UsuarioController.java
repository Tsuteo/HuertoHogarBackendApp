package com.huertohogar.backend.controller;

import com.huertohogar.backend.dto.LoginRequest;
import com.huertohogar.backend.dto.ResetPasswordRequest;
import com.huertohogar.backend.model.Usuario;
import com.huertohogar.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    // Registrar nuevo usuario
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@Valid @RequestBody Usuario usuario) {
        // Verificar si el correo ya existe
        if (repository.existsByCorreo(usuario.getCorreo())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "El correo ya está registrado");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Por defecto, asignar rol CLIENTE si no se especifica
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("CLIENTE");
        }

        Usuario nuevoUsuario = repository.save(usuario);
        
        // No devolver la contraseña en la respuesta
        nuevoUsuario.setContrasena(null);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    // Iniciar sesión
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<Usuario> usuarioOpt = repository.findByCorreo(loginRequest.getCorreo());

        if (usuarioOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Correo o contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Usuario usuario = usuarioOpt.get();

        // Verificar contraseña (en producción, usar BCrypt)
        if (!usuario.getContrasena().equals(loginRequest.getContrasena())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Correo o contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // No devolver la contraseña
        usuario.setContrasena(null);
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);

        return ResponseEntity.ok(usuario);
    }

    // Solicitar reseteo de contraseña
    @PostMapping("/solicitar-reseteo")
    public ResponseEntity<?> solicitarReseteo(@RequestParam String correo) {
        Optional<Usuario> usuarioOpt = repository.findByCorreo(correo);

        if (usuarioOpt.isEmpty()) {
            // Por seguridad, no revelar si el correo existe
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Si el correo existe, recibirás un código de reseteo");
            return ResponseEntity.ok(response);
        }

        Usuario usuario = usuarioOpt.get();

        // Generar token de 6 dígitos
        String token = String.format("%06d", (int)(Math.random() * 1000000));
        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Válido por 15 minutos

        repository.save(usuario);

        // En producción, enviar el token por email
        // Aquí lo devolvemos directamente para pruebas
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Código de reseteo generado");
        response.put("token", token); // Solo para desarrollo, remover en producción
        response.put("correo", correo);

        return ResponseEntity.ok(response);
    }

    // Resetear contraseña con token
    @PostMapping("/resetear-contrasena")
    public ResponseEntity<?> resetearContrasena(@Valid @RequestBody ResetPasswordRequest request) {
        Optional<Usuario> usuarioOpt = repository.findByResetToken(request.getToken());

        if (usuarioOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token inválido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Usuario usuario = usuarioOpt.get();

        // Verificar si el token expiró
        if (usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "El token ha expirado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Actualizar contraseña
        usuario.setContrasena(request.getNuevaContrasena());
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);

        repository.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Contraseña actualizada exitosamente");

        return ResponseEntity.ok(response);
    }

    // Obtener todos los usuarios (admin)
    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerTodos() {
        List<Usuario> usuarios = repository.findAll();
        // No devolver contraseñas ni tokens
        usuarios.forEach(u -> {
            u.setContrasena(null);
            u.setResetToken(null);
            u.setResetTokenExpiry(null);
        });
        return ResponseEntity.ok(usuarios);
    }

    // Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Optional<Usuario> usuario = repository.findById(id);
        if (usuario.isPresent()) {
            Usuario u = usuario.get();
            u.setContrasena(null);
            u.setResetToken(null);
            u.setResetTokenExpiry(null);
            return ResponseEntity.ok(u);
        }
        return ResponseEntity.notFound().build();
    }

    // Actualizar perfil de usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody Usuario usuarioActualizado) {
        Optional<Usuario> usuarioExistente = repository.findById(id);

        if (usuarioExistente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioExistente.get();
        
        // Actualizar solo campos permitidos
        usuario.setNombre(usuarioActualizado.getNombre());
        usuario.setApellido(usuarioActualizado.getApellido());
        usuario.setDireccion(usuarioActualizado.getDireccion());
        
        // Solo actualizar contraseña si se proporciona una nueva
        if (usuarioActualizado.getContrasena() != null && !usuarioActualizado.getContrasena().isEmpty()) {
            usuario.setContrasena(usuarioActualizado.getContrasena());
        }

        Usuario usuarioGuardado = repository.save(usuario);
        usuarioGuardado.setContrasena(null);
        usuarioGuardado.setResetToken(null);
        usuarioGuardado.setResetTokenExpiry(null);

        return ResponseEntity.ok(usuarioGuardado);
    }

    // Eliminar usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
