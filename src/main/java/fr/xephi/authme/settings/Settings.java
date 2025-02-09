package fr.xephi.authme.settings;

import ch.jalu.configme.SettingsManagerImpl;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.MigrationService;
import ch.jalu.configme.resource.PropertyResource;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * The AuthMe settings manager.
 */
public class Settings extends SettingsManagerImpl {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(Settings.class);
    private final File pluginFolder;
    private String passwordEmailMessage;
    private String verificationEmailMessage;
    private String recoveryCodeEmailMessage;
    private String shutdownEmailMessage;
    private String newPasswordEmailMessage;

    /**
     * Constructor.
     *
     * @param pluginFolder the AuthMe plugin folder
     * @param resource the property resource to read and write properties to
     * @param migrationService migration service to check the settings file with
     * @param configurationData configuration data (properties and comments)
     */
    public Settings(File pluginFolder, PropertyResource resource, MigrationService migrationService,
                    ConfigurationData configurationData) {
        super(resource, configurationData, migrationService);
        this.pluginFolder = pluginFolder;
        loadSettingsFromFiles();
    }

    /**
     * Return the text to use in email registrations.
     *
     * @return The email message
     */
    public String getPasswordEmailMessage() {
        return passwordEmailMessage;
    }

    /**
     * Return the text for verification emails (before sensitive commands can be used).
     *
     * @return The email message
     */
    public String getVerificationEmailMessage() {
        return verificationEmailMessage;
    }

    /**
     * Return the text to use when someone requests to receive a recovery code.
     *
     * @return The email message
     */
    public String getRecoveryCodeEmailMessage() {
        return recoveryCodeEmailMessage;
    }

    public String getShutdownEmailMessage() {
        return shutdownEmailMessage;
    }

    public String getNewPasswordEmailMessage() {
        return newPasswordEmailMessage;
    }

    private void loadSettingsFromFiles() {
        newPasswordEmailMessage = readFile("new_email.html");
        passwordEmailMessage = readFile("email.html");
        verificationEmailMessage = readFile("verification_code_email.html");
        recoveryCodeEmailMessage = readFile("recovery_code_email.html");
        shutdownEmailMessage = readFile("shutdown.html");
    }

    @Override
    public void reload() {
        super.reload();
        loadSettingsFromFiles();
    }

    /**
     * Reads a file from the plugin folder or copies it from the JAR to the plugin folder.
     *
     * @param filename the file to read
     * @return the file's contents
     */
    private String readFile(String filename) {
        final File file = new File(pluginFolder, filename);
        if (copyFileFromResource(file, filename)) {
            try {
                return Files.asCharSource(file, StandardCharsets.UTF_8).read();
            } catch (IOException e) {
                logger.logException("Failed to read file '" + filename + "':", e);
            }
        } else {
            logger.warning("Failed to copy file '" + filename + "' from JAR");
        }
        return "";
    }
}
