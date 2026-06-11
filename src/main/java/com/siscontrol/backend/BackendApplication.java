package com.siscontrol.backend;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- IMPORTANTE

import java.time.LocalDateTime;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // <-- ACTIVA EL MOTOR DE TAREAS AUTOMÁTICAS EN SEGUNDO PLANO
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Este metodo se ejecuta antes que cualquier otra lógica en Spring.
	 * Fuerza a la Máquina Virtual de Java (JVM) a trabajar en la Zona Horaria de
	 * Chile.
	 */
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
		System.out.println(">>> ZONA HORARIA CONFIGURADA EN: " + TimeZone.getDefault().getID());
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
				admin.setRut("11111111-1");

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
				guard.setRole(UserRole.GUARD);
				guard.setStatus(1);
				guard.setRut("22222222-2");

				guard.setCreatedAt(LocalDateTime.now());
				guard.setCreatedBy(0L);

				userRepository.save(guard);
				System.out.println(">>> SEEDER: Usuario GUARDIA creado (pass: guardia123)");
			}
		};
	}
}