package br.com.fiap.parquimetro;

import br.com.fiap.parquimetro.entities.Estacionamento;
import br.com.fiap.parquimetro.entities.pagamento.ModalidadeTempoEnum;
import br.com.fiap.parquimetro.service.EstacionamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


@SpringBootApplication
public class ParquimetroApplication implements CommandLineRunner {
	@Autowired
	private EstacionamentoService estacionamentoService;

	public static void main(String[] args) {
		SpringApplication.run(ParquimetroApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Timer timer = new Timer();

		final long SEGUNDOS = 1000;
		TimerTask tarefa = new TimerTask() {
			@Override
			public void run() {
				List<Estacionamento> listaAberto = estacionamentoService.buscaEmAberto();
				if (listaAberto != null && !listaAberto.isEmpty() ) {
					for (Estacionamento batida : listaAberto) {
						if (batida.getModalidadeTempoEnum()
								.equals(ModalidadeTempoEnum.TEMPO_FIXO)) {
							emiteTempoFixo(batida);
						} else {
							emiteTempoVarivel(batida);
						}
					}
				}
			}

			private void emiteTempoFixo(Estacionamento batida) {
				LocalDateTime tempoAtual = LocalDateTime.now();
				LocalDateTime tempoBatida = batida.getHorarioEntrada();

				//Só testa de data mes e ano forem iguais
				if (tempoAtual.getDayOfMonth() == tempoBatida.getDayOfMonth()
					&& tempoAtual.getMonth() == tempoBatida.getMonth()
						&& tempoAtual.getYear() == tempoBatida.getYear()) {

					Long minutosBatida = batida.getTempoEmHoras()
							+ (tempoBatida.getHour() * 60)
							+ tempoBatida.getMinute();

					Long minutosAtual = (long) (tempoAtual.getHour() * 60
                                                + tempoAtual.getMinute());

					if (minutosBatida - minutosAtual <= 15
							&& batida.getFlagAlerta()==0) {
						System.out.println("Tempo está expirando - email enviado para : "
								+ batida.getCarro().getPessoa().getEmail());
						estacionamentoService.setarAlerta(batida.getId());
					}
				}
			}

			private void emiteTempoVarivel(Estacionamento batida) {
				LocalDateTime tempoAtual = LocalDateTime.now();
				LocalDateTime tempoBatida = batida.getHorarioEntrada();

				//Só testa de data mes e ano forem iguais
				if (tempoAtual.getDayOfMonth() == tempoBatida.getDayOfMonth()
						&& tempoAtual.getMonth() == tempoBatida.getMonth()
						&& tempoAtual.getYear() == tempoBatida.getYear()) {

					Long minutosBatida = batida.getTempoEmHoras()
							+ (tempoBatida.getHour() * 60)
							+ tempoBatida.getMinute();

					Long minutosAtual = (long) (tempoAtual.getHour() * 60
							+ tempoAtual.getMinute());

					if (minutosBatida - minutosAtual < 0) {
						System.out.println("Tempo será incrementado - email enviado para : "
								+ batida.getCarro().getPessoa().getEmail());
						estacionamentoService.aumentarTempo(batida.getId(), batida.getTempoEmHoras() + 60);
					}
				}
			}
		};
		timer.scheduleAtFixedRate(tarefa, 0, SEGUNDOS);
	}


}
