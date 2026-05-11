package com.siscontrol.backend;

import com.siscontrol.backend.enums.UserRole;
import com.siscontrol.backend.enums.UserStatus;
import com.siscontrol.backend.models.User;
import com.siscontrol.backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Este Bean se ejecuta automáticamente al arrancar la aplicación.
	 * Crea un usuario ADMIN inicial si la base de datos está vacía.
	 */
	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository) {
		return args -> {
			String adminUsername = "admin";

			if (userRepository.findByUsername(adminUsername).isEmpty()) {
				User admin = new User();
				admin.setUsername(adminUsername);
				admin.setPassword("admin123"); // En el futuro usaremos BCrypt aquí
				admin.setEmail("admin@siscontrol.com");
				admin.setFullName("Administrador Inicial");
				admin.setRole(UserRole.ADMIN);
				admin.setStatus(UserStatus.ACTIVE);

				userRepository.save(admin);
				System.out.println("--------------------------------------");
				System.out.println(">>> SEEDER: Usuario ADMIN creado con éxito.");
				System.out.println(">>> Usuario: admin | Password: admin123");
				System.out.println("--------------------------------------");
			} else {
				System.out.println(">>> SEEDER: El usuario ADMIN ya existe. No se realizaron cambios.");
			}
		};
	}
}