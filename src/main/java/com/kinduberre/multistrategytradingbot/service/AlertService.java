package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
    public class AlertService extends TelegramLongPollingBot {

        private final JavaMailSender mailSender;
        private final ConcurrentLinkedQueue<Alert> alertHistory = new ConcurrentLinkedQueue<>();

        @Value("${alerts.email.enabled}")
        private boolean emailEnabled;

        @Value("${alerts.email.recipients}")
        private List<String> emailRecipients;

        @Value("${alerts.telegram.enabled}")
        private boolean telegramEnabled;

        @Value("${alerts.telegram.bot-token}")
        private String botToken;

        @Value("${alerts.telegram.chat-id}")
        private String chatId;

        public void sendAlert(Alert alert) {
            // Log alert
            logAlert(alert);

            // Store in history
            alertHistory.offer(alert);
            if (alertHistory.size() > 1000) {
                alertHistory.poll();
            }

            // Send notifications based on severity
            if (alert.getSeverity() == Alert.Severity.CRITICAL ||
                    alert.getSeverity() == Alert.Severity.WARNING) {

                if (emailEnabled) {
                    sendEmailAlert(alert);
                }

                if (telegramEnabled) {
                    sendTelegramAlert(alert);
                }
            }
        }

        private void logAlert(Alert alert) {
            String logMessage = String.format("[%s] %s: %s - %s",
                    alert.getSeverity(), alert.getType(), alert.getMessage(), alert.getDetails());

            switch (alert.getSeverity()) {
                case CRITICAL:
                    log.error(logMessage);
                    break;
                case WARNING:
                    log.warn(logMessage);
                    break;
                case INFO:
                    log.info(logMessage);
                    break;
            }
        }

        private void sendEmailAlert(Alert alert) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(emailRecipients.toArray(new String[0]));
                message.setSubject(String.format("Trading Bot Alert: %s - %s",
                        alert.getSeverity(), alert.getType()));
                message.setText(formatAlertMessage(alert));

                mailSender.send(message);
            } catch (Exception e) {
                log.error("Failed to send email alert", e);
            }
        }

        private void sendTelegramAlert(Alert alert) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(formatTelegramMessage(alert));
                message.setParseMode("HTML");

                execute(message);
            } catch (Exception e) {
                log.error("Failed to send Telegram alert", e);
            }
        }

        private String formatAlertMessage(Alert alert) {
            return String.format(
                    "Alert Type: %s\nSeverity: %s\nMessage: %s\nDetails: %s\nTime: %s",
                    alert.getType(), alert.getSeverity(), alert.getMessage(),
                    alert.getDetails(), alert.getTimestamp()
            );
        }

        private String formatTelegramMessage(Alert alert) {
            String emoji = getEmojiForSeverity(alert.getSeverity());
            return String.format(
                    "%s <b>%s Alert</b>\n\n<b>Type:</b> %s\n<b>Message:</b> %s\n<b>Details:</b> %s\n<b>Time:</b> %s",
                    emoji, alert.getSeverity(), alert.getType(), alert.getMessage(),
                    alert.getDetails(), alert.getTimestamp()
            );
        }

        private String getEmojiForSeverity(Alert.Severity severity) {
            switch (severity) {
                case CRITICAL: return "🚨";
                case WARNING: return "⚠️";
                case INFO: return "ℹ️";
                default: return "📊";
            }
        }

        @Override
        public String getBotUsername() {
            return "TradingBot";
        }

        @Override
        public String getBotToken() {
            return botToken;
        }

        @Override
        public void onUpdateReceived(Update update) {
            // Handle incoming commands if needed
        }
}
