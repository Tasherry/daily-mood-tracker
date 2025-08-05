package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private JHipsterProperties.Mail mailProperties;

    @InjectMocks
    private MailService mailService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setLangKey("en");
        user.setFirstName("Test");
        user.setLastName("User");
    }

    @Test
    void shouldSendEmailSuccessfully() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = false;
        boolean isHtml = true;

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldHandleMailException() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = false;
        boolean isHtml = true;

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Mail error") {}).when(javaMailSender).send(mimeMessage);

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
        // Should not throw exception, just log warning
    }

    @Test
    void shouldHandleMessagingException() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = false;
        boolean isHtml = true;

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Messaging error") {}).when(javaMailSender).send(mimeMessage);

        // When & Then - Should not throw exception, just log warning
        assertThatCode(() -> mailService.sendEmail(to, subject, content, isMultipart, isHtml)).doesNotThrowAnyException();

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSendEmailFromTemplate() throws Exception {
        // Given
        String templateName = "activationEmail";
        String titleKey = "email.activation.title";
        String subject = "Activation Email";
        String content = "<html>Activation email content</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq(titleKey), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(content);

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine).process(eq(templateName), any(Context.class));
        verify(messageSource).getMessage(eq(titleKey), any(), any(Locale.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldNotSendEmailWhenUserEmailIsNull() {
        // Given
        user.setEmail(null);
        String templateName = "activationEmail";
        String titleKey = "email.activation.title";

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendActivationEmail() {
        // Given
        String subject = "Activation Email";
        String content = "<html>Activation email content</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq("email.activation.title"), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq("mail/activationEmail"), any(Context.class))).thenReturn(content);

        // When
        mailService.sendActivationEmail(user);

        // Then
        verify(templateEngine).process(eq("mail/activationEmail"), any(Context.class));
        verify(messageSource).getMessage(eq("email.activation.title"), any(), any(Locale.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSendCreationEmail() {
        // Given
        String subject = "Creation Email";
        String content = "<html>Creation email content</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq("email.activation.title"), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq("mail/creationEmail"), any(Context.class))).thenReturn(content);

        // When
        mailService.sendCreationEmail(user);

        // Then
        verify(templateEngine).process(eq("mail/creationEmail"), any(Context.class));
        verify(messageSource).getMessage(eq("email.activation.title"), any(), any(Locale.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSendPasswordResetMail() {
        // Given
        String subject = "Password Reset Email";
        String content = "<html>Password reset email content</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq("email.reset.title"), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq("mail/passwordResetEmail"), any(Context.class))).thenReturn(content);

        // When
        mailService.sendPasswordResetMail(user);

        // Then
        verify(templateEngine).process(eq("mail/passwordResetEmail"), any(Context.class));
        verify(messageSource).getMessage(eq("email.reset.title"), any(), any(Locale.class));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldHandleDifferentLocales() {
        // Given
        user.setLangKey("fr");
        String templateName = "activationEmail";
        String titleKey = "email.activation.title";
        String subject = "Email d'activation";
        String content = "<html>Contenu de l'email d'activation</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq(titleKey), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(content);

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine).process(eq(templateName), any(Context.class));
        verify(messageSource).getMessage(eq(titleKey), any(), eq(Locale.forLanguageTag("fr")));
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSendMultipartEmail() throws Exception {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        boolean isMultipart = true;
        boolean isHtml = false;

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        mailService.sendEmail(to, subject, content, isMultipart, isHtml);

        // Then
        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSetCorrectContextVariables() {
        // Given
        String templateName = "activationEmail";
        String titleKey = "email.activation.title";
        String subject = "Activation Email";
        String content = "<html>Activation email content</html>";

        when(jHipsterProperties.getMail()).thenReturn(mailProperties);
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        when(mailProperties.getBaseUrl()).thenReturn("http://localhost:8080");
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(eq(titleKey), any(), any(Locale.class))).thenReturn(subject);
        when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(content);

        // When
        mailService.sendEmailFromTemplate(user, templateName, titleKey);

        // Then
        verify(templateEngine).process(
            eq(templateName),
            argThat(context -> {
                assertThat(context.getVariable("user")).isEqualTo(user);
                assertThat(context.getVariable("baseUrl")).isEqualTo("http://localhost:8080");
                return true;
            })
        );
    }
}
