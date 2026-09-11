package portalmissionario.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import portalmissionario.entity.DadosMissionariosEntity;

import java.util.regex.Pattern;

// "Enviar Email" -- dispara um email de verdade pro missionário via Quarkus Mailer (SMTP
// relay do Brevo em fase de testes, ver application.properties). O endereço do membro que
// escreve NUNCA é persistido (decisão explícita do usuário) -- se informado, entra tanto como
// texto visível no corpo do email (pedido explícito do usuário, já que o header Reply-To
// sozinho é invisível na leitura normal) quanto como header Reply-To de verdade, pra quem usa
// "Responder" no cliente de email já cair no lugar certo.
@ApplicationScoped
public class EnvioEmailService {

    private static final int MENSAGEM_MAX_CARACTERES = 10000;

    // Validação frouxa só pra pegar erro de digitação óbvio antes de gastar uma chamada SMTP
    // -- não segue RFC 5322 à risca de proposito, mesma filosofia de simplicidade do resto do
    // projeto.
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Inject
    EntityManager entityManager;

    @Inject
    Mailer mailer;

    @Transactional
    public void enviarEmailParaMissionario(Long missionarioId, String remetenteNome, String remetenteUnidade,
                                            String mensagem, String emailRemetenteOpcional) {
        if (mensagem == null || mensagem.trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem é obrigatória.");
        }
        if (mensagem.length() > MENSAGEM_MAX_CARACTERES) {
            throw new IllegalArgumentException("A mensagem excede o limite de " + MENSAGEM_MAX_CARACTERES + " caracteres.");
        }

        String emailRemetenteTratado = emailRemetenteOpcional == null ? null : emailRemetenteOpcional.trim();
        if (emailRemetenteTratado != null && !emailRemetenteTratado.isEmpty() && !EMAIL_PATTERN.matcher(emailRemetenteTratado).matches()) {
            throw new IllegalArgumentException("O endereço de email informado não parece válido.");
        }

        DadosMissionariosEntity missionario = entityManager.find(DadosMissionariosEntity.class, missionarioId);
        if (missionario == null) {
            throw new NotFoundException("Missionário não encontrado: " + missionarioId);
        }
        if (missionario.getEmail() == null || missionario.getEmail().isBlank()) {
            throw new IllegalArgumentException("Este missionário ainda não tem email cadastrado.");
        }

        String assunto = "Nova mensagem via Portal Missionário de " + remetenteNome;
        StringBuilder corpo = new StringBuilder(mensagem.trim())
                .append("\n\n— Enviado por ").append(remetenteNome).append(" (").append(remetenteUnidade)
                .append(") através do Portal Missionário da Estaca Betim.");

        boolean temEmailRemetente = emailRemetenteTratado != null && !emailRemetenteTratado.isEmpty();
        if (temEmailRemetente) {
            // Visível no corpo, não só no header Reply-To -- o Reply-To sozinho é invisível na
            // leitura normal do email (só aparece se o missionário clicar em "Responder" ou
            // abrir os cabeçalhos originais), pedido explícito do usuário pra deixar claro pra
            // onde responder mesmo sem o missionário saber procurar isso.
            corpo.append("\nPor gentileza responder mensagem, usando o email: ").append(emailRemetenteTratado);
        }

        Mail email = Mail.withText(missionario.getEmail(), assunto, corpo.toString());
        if (temEmailRemetente) {
            email.addHeader("Reply-To", emailRemetenteTratado);
        }

        mailer.send(email);
    }

}
