package fr.xephi.authme.data;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VerificationCodeManager implements SettingsDependent, HasCleanup {

    private final EmailService emailService;
    private final DataSource dataSource;
    private final PermissionsManager permissionsManager;

    private final ExpiringMap<String, String> verificationCodes;
    private final Set<String> verifiedPlayers;

    private boolean canSendMail;
    private final String dateFormat;

    @Inject
    VerificationCodeManager(Settings settings, DataSource dataSource, EmailService emailService,
                            PermissionsManager permissionsManager) {
        this.emailService = emailService;
        this.dataSource = dataSource;
        this.permissionsManager = permissionsManager;
        verifiedPlayers = new HashSet<>();
        this.dateFormat = settings.getProperty(PluginSettings.DATE_FORMAT);
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes = new ExpiringMap<>(countTimeout, TimeUnit.MINUTES);
        reload(settings);
    }

    /**
     * Returns if it is possible to send emails
     *
     * @return true if the service is enabled, false otherwise
     */
    public boolean canSendMail() {
        return canSendMail;
    }

    /**
     * Returns whether the given player is able to verify his identity
     *
     * @param player the player to verify
     * @return true if the player has not been verified yet, false otherwise
     */
    public boolean isVerificationRequired(Player player) {
        final String name = player.getName();
        return canSendMail
            && !isPlayerVerified(name)
            && permissionsManager.hasPermission(player, PlayerPermission.VERIFICATION_CODE)
            && hasEmail(name);
    }

    /**
     * Returns whether the given player is required to verify his identity through a command
     *
     * @param name the name of the player to verify
     * @return true if the player has an existing code and has not been verified yet, false otherwise
     */
    public boolean isCodeRequired(String name) {
        return canSendMail && hasCode(name) && !isPlayerVerified(name);
    }

    /**
     * Returns whether the given player has been verified or not
     *
     * @param name the name of the player to verify
     * @return true if the player has been verified, false otherwise
     */
    private boolean isPlayerVerified(String name) {
        return verifiedPlayers.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns if a code exists for the player
     *
     * @param name the name of the player to verify
     * @return true if the code exists, false otherwise
     */
    public boolean hasCode(String name) {
        return (verificationCodes.get(name.toLowerCase(Locale.ROOT)) != null);
    }

    /**
     * Returns whether the given player is able to receive emails
     *
     * @param name the name of the player to verify
     * @return true if the player is able to receive emails, false otherwise
     */
    public boolean hasEmail(String name) {
        boolean result = false;
        DataSourceValue<String> emailResult = dataSource.getEmail(name);
        if (emailResult.rowExists()) {
            final String email = emailResult.getValue();
            if (!Utils.isEmailEmpty(email)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Check if a code exists for the player or generates and saves a new one.
     *
     * @param name the player's name
     */
    public void codeExistOrGenerateNew(String name) {
        if (!hasCode(name)) {
            generateCode(name);
        }
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     */
    private void generateCode(String name) {
        DataSourceValue<String> emailResult = dataSource.getEmail(name);
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
        Date date = new Date(System.currentTimeMillis());
        if (emailResult.rowExists()) {
            final String email = emailResult.getValue();
            if (!Utils.isEmailEmpty(email)) {
                String code = RandomStringUtils.generateNum(6); // 6 digits code
                verificationCodes.put(name.toLowerCase(Locale.ROOT), code);
                emailService.sendVerificationMail(name, email, code, dateFormat.format(date));
            }
        }
    }

    /**
     * Checks the given code against the existing one.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    public boolean checkCode(String name, String code) {
        boolean correct = false;
        if (code.equals(verificationCodes.get(name.toLowerCase(Locale.ROOT)))) {
            verify(name);
            correct = true;
        }
        return correct;
    }

    /**
     * Add the user to the set of verified users
     *
     * @param name the name of the player to generate a code for
     */
    public void verify(String name) {
        verifiedPlayers.add(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Remove the user from the set of verified users
     *
     * @param name the name of the player to generate a code for
     */
    public void unverify(String name){
        verifiedPlayers.remove(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public void reload(Settings settings) {
        canSendMail = emailService.hasAllInformation();
        long countTimeout = settings.getProperty(SecuritySettings.VERIFICATION_CODE_EXPIRATION_MINUTES);
        verificationCodes.setExpiration(countTimeout, TimeUnit.MINUTES);
    }

    @Override
    public void performCleanup() {
        verificationCodes.removeExpiredEntries();
    }
}
