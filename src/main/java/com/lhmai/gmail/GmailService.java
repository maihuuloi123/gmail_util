package com.lhmai.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class GmailService implements MailerService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final List<String> SCOPES = new ArrayList<>(Arrays.asList(GmailScopes.GMAIL_SEND));

    private final HttpTransport httpTransport;
    private final Credential credentials;
    private String applicationName;
    private static String secretFilePath;
    private static String dataStorePath;
    private String hostMail;

    public GmailService(HttpTransport httpTransport, ConfigProperties properties) throws Exception {
        secretFilePath = properties.getCredentialFile();
        dataStorePath = properties.getDataStoreDir();
        applicationName = properties.getAppName();
        hostMail = properties.getHostMail();

        this.httpTransport = httpTransport;
        credentials = authorize();
    }

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize() throws Exception {
        // load client secrets
        InputStream in = Files.newInputStream(Paths.get(secretFilePath));

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(in));
        // set up authorization code flow
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
                SCOPES).setDataStoreFactory(new FileDataStoreFactory(new File(dataStorePath)))
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }


    public boolean send(Email email) {
        try {
            // set style for email

            setStyleForEmail(email);

            MimeMessage mimeMessage =
                    createEmail(hostMail, email.getSubject(), email.getText(), email.isHtml(), email.getCarbonCopies(), email.getTos());

            Message message = createMessageWithEmail(mimeMessage);
            return createGmail().users()
                    .messages()
                    .send("me", message)
                    .execute()
                    .getLabelIds().contains("SENT");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setStyleForEmail(Email email) {
        if (email.isHtml()) {
//            String text = email.getText();
//            String newTextWithStyle = String.format("<span style="font-size: 18px !important;">%s</span>", text);
//            email.setText(newTextWithStyle);
        }
    }

    public static Message createMessageWithEmail1(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private Gmail createGmail() throws Exception {
        return new Gmail.Builder(httpTransport, JSON_FACTORY, this.credentials)
                .setApplicationName(applicationName)
                .build();
    }

    private MimeMessage createEmail(String from, String subject, String body, boolean isHtml, List<String> carbonCopies, String... tos) throws MessagingException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
        email.setFrom(new InternetAddress(from));
        for (String to : tos) {
            email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        }

        for (String carbonCopy : carbonCopies) {
            email.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(carbonCopy));

        }

        email.setSubject(subject);
        if (isHtml) {
            email.setContent(body, "text/html; charset=utf-8");
        } else {
            email.setText(body);
        }
        return email;
    }

    public boolean send(Email emailDTO, File file, String fileName) {
        try {
            setStyleForEmail(emailDTO);

            MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties(), null));

            for (String to : emailDTO.getTos()) {
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
            }

            email.setSubject(emailDTO.getSubject());

            MimeBodyPart mimeBodyPart = new MimeBodyPart();


            if (emailDTO.isHtml()) {
                mimeBodyPart.setContent(emailDTO.getText(), "text/html; charset=utf-8");
            } else {
                mimeBodyPart.setContent(emailDTO.getText(), "text/plain");
            }

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            mimeBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);

            mimeBodyPart.setDataHandler(new DataHandler(source));
            mimeBodyPart.setFileName(fileName);

            multipart.addBodyPart(mimeBodyPart);
            email.setContent(multipart);

            Message message = createMessageWithEmail1(email);
            message = createGmail().users()
                    .messages()
                    .send("me", message)
                    .execute();

            return true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);

        return new Message()
                .setRaw(Base64.encodeBase64URLSafeString(buffer.toByteArray()));
    }


    public static void main(String[] args) throws Exception {

        GmailCredential credential = new GmailCredential();
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GmailService gmailService = new GmailService(httpTransport, new ConfigProperties());

        Email email = new Email();
        email.setHtml(true);
        email.setSubject("My Yinyang test");
        email.setText("<span><b>My text</b></span>");
        email.setEmailTos("huuloiofficial@gmail.com");
        gmailService.send(email);
    }

}