package com.task.task_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskBackendApplicationTests {

	@Test
	@DisplayName("Deve carregar o contexto da aplicação com o perfil de teste")
	void deveCarregarContexto() {
	}

}
