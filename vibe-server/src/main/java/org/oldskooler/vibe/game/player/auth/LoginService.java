package org.oldskooler.vibe.game.player.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.game.player.PlayerManager;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;

/**
 * Authenticates login requests and binds the resulting player to the session.
 */
public final class LoginService {

    private static final Logger log = LogManager.getLogger(LoginService.class);

    private final LoginResponseWriter responses;

    /**
     * Creates a new LoginService.
     * @param responses the responses value
     */
    public LoginService(LoginResponseWriter responses) {
        this.responses = responses;
    }

    /**
     * Tries login.
     * @param session the session value
     * @param username the username value
     * @param password the password value
     */
    public void tryLogin(Session session, String username, String password) {
        log.info("Login attempt: {}", username);

        UserEntity user = UserDao.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            responses.sendLoginFailure(session);
            log.warn("Login failed for: {}", username);
            return;
        }

        completeLogin(session, user);
    }

    /**
     * Logins with ticket.
     * @param session the session value
     * @param ticket the ticket value
     */
    public void loginWithTicket(Session session, String ticket) {
        log.info("SSO login attempt with ticket: {}...", ticket.length() > 8 ? ticket.substring(0, 8) : ticket);

        UserEntity user = UserDao.findBySsoTicket(ticket);
        if (user == null) {
            responses.sendLoginFailure(session);
            log.warn("SSO login failed");
            return;
        }

        completeLogin(session, user);
    }

    /**
     * Completes login.
     * @param session the session value
     * @param user the user value
     */
    private void completeLogin(Session session, UserEntity user) {
        responses.sendLoginSuccess(session, user);
        PlayerManager.getInstance().register(session);
        log.info("User logged in: {} (id={})", user.getUsername(), user.getId());
    }
}
