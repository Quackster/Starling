package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;
import java.time.Instant;

@Entity(table = "users")
public class UserEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, type = "TEXT")
    private String password;

    @Column(nullable = false)
    private String figure;

    @Column(name = "pool_figure", nullable = false)
    private String poolFigure = "";

    @Column(nullable = false, length = 1)
    private String sex = "M";

    @Column(nullable = false, length = 100)
    private String motto = "";

    @Column(nullable = false)
    private String email = "";

    @Column(nullable = false)
    private int credits = 50;

    @Column(nullable = false, defaultValue = "0")
    private int pixels;

    @Column(nullable = false, defaultValue = "0")
    private int tickets;

    @Column(nullable = false, defaultValue = "0")
    private int film;

    @Column(name = "rank", nullable = false)
    private int rank = 1;

    @Column(name = "last_online", nullable = false)
    private java.sql.Timestamp lastOnline;

    @Column(name = "remember_token")
    private String rememberToken;

    @Column(name = "is_online")
    private Integer isOnline = 0;

    @Column(name = "created_at", nullable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.sql.Timestamp updatedAt;

    @Column(name = "sso_ticket")
    private String ssoTicket;

    @Column(name = "machine_id", nullable = false, type = "TEXT")
    private String machineId = "";

    @Column(name = "club_subscribed", nullable = false, defaultValue = "0")
    private long clubSubscribed;

    @Column(name = "club_expiration", nullable = false, defaultValue = "0")
    private long clubExpiration;

    @Column(name = "club_gift_due", nullable = false, defaultValue = "0")
    private long clubGiftDue;

    @Column(name = "allow_stalking", nullable = false)
    private int allowStalking = 1;

    @Column(name = "allow_friend_requests", nullable = false)
    private int allowFriendRequests = 1;

    @Column(name = "online_status_visible", nullable = false)
    private int onlineStatusVisible = 1;

    @Column(name = "profile_visible", nullable = false)
    private int profileVisible = 1;

    @Column(name = "wordfilter_enabled", nullable = false)
    private int wordfilterEnabled = 1;

    @Column(name = "trade_enabled", nullable = false, defaultValue = "0")
    private int tradeEnabled;

    @Column(name = "trade_ban_expiration", nullable = false, defaultValue = "0")
    private long tradeBanExpiration;

    @Column(name = "sound_enabled", nullable = false)
    private int soundEnabled = 1;

    @Column(name = "selected_room_id", nullable = false, defaultValue = "0")
    private int selectedRoomId;

    @Column(name = "tutorial_finished", nullable = false, defaultValue = "0")
    private int tutorialFinished;

    @Column(name = "daily_coins_enabled", nullable = false, defaultValue = "0")
    private int dailyCoinsEnabled;

    @Column(name = "daily_respect_points", nullable = false)
    private int dailyRespectPoints = 3;

    @Column(name = "respect_points", nullable = false, defaultValue = "0")
    private int respectPoints;

    @Column(name = "respect_day", nullable = false)
    private String respectDay = "";

    @Column(name = "respect_given", nullable = false, defaultValue = "0")
    private int respectGiven;

    @Column(name = "totem_effect_expiry", nullable = false, defaultValue = "0")
    private long totemEffectExpiry;

    @Column(name = "favourite_group", nullable = false, defaultValue = "0")
    private int favouriteGroup;

    @Column(name = "home_room", nullable = false, defaultValue = "0")
    private int homeRoom;

    @Column(name = "has_flash_warning", nullable = false)
    private int hasFlashWarning = 1;

    public UserEntity() {}

    public static UserEntity createDefaultAdmin() {
        UserEntity user = new UserEntity();
        Timestamp now = Timestamp.from(Instant.now());

        user.username = "admin";
        user.password = "admin";
        user.figure = "hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61";
        user.sex = "M";
        user.motto = "Hello Habbo!";
        user.email = "admin@starling.local";
        user.credits = 10000;
        user.lastOnline = now;
        user.createdAt = now;
        user.updatedAt = now;
        user.ssoTicket = "starling-sso-ticket";

        return user;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFigure() { return figure; }
    public String getPoolFigure() { return poolFigure; }
    public String getSex() { return sex; }
    public String getMotto() { return motto; }
    public int getCredits() { return credits; }
    public int getRank() { return rank; }
    public int getTickets() { return tickets; }
    public int getFilm() { return film; }
    public String getSsoTicket() { return ssoTicket; }
    public int getSoundEnabled() { return soundEnabled; }
    public int getSelectedRoomId() { return selectedRoomId; }
    public int getHomeRoom() { return homeRoom; }

    public void setSelectedRoomId(int selectedRoomId) { this.selectedRoomId = selectedRoomId; }
    public void setHomeRoom(int homeRoom) { this.homeRoom = homeRoom; }
}
