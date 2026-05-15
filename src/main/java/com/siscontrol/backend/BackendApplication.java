package com.siscontrol.backend;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableJpaAuditing
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository) {
		return args -> {
			// 1. CREAR ADMIN (Para gestión web)
			String adminUsername = "admin";
			if (userRepository.findByUsername(adminUsername).isEmpty()) {
				User admin = new User();
				admin.setUsername(adminUsername);
				admin.setPassword("admin123");
				admin.setEmail("admin@siscontrol.com");
				admin.setFullName("Administrador Inicial");
				admin.setRole(UserRole.ADMIN);
				admin.setStatus(1);
				admin.setRut("11.111.111-1"); // <--- CORRECCIÓN: Evita el error "rut cannot be null"

				admin.setCreatedAt(LocalDateTime.now());
				admin.setCreatedBy(0L);

				userRepository.save(admin);
				System.out.println(">>> SEEDER: Usuario ADMIN creado (pass: admin123)");
			}

			// 2. CREAR GUARDIA DE PRUEBA (Para usar en Android Studio)
			String guardUsername = "guardia1";
			if (userRepository.findByUsername(guardUsername).isEmpty()) {
				User guard = new User();
				guard.setUsername(guardUsername);
				guard.setPassword("guardia123");
				guard.setEmail("guardia1@siscontrol.com");
				guard.setFullName("Juan Guardia");
				guard.setRole(UserRole.GUARD); // Asegúrate de que GUARD esté en tu enum UserRole
				guard.setStatus(1);
				guard.setRut("22.222.222-2"); // <--- RUT obligatorio para el guardia

				guard.setCreatedAt(LocalDateTime.now());
				guard.setCreatedBy(0L);

				userRepository.save(guard);
				System.out.println(">>> SEEDER: Usuario GUARDIA creado (pass: guardia123)");
			}
		};
	}
}