package org.oldskooler.vibe.web.settings;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "web_settings")
public class WebSettingEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey = "";

    @Column(name = "setting_value", nullable = false, type = "LONGTEXT")
    private String settingValue = "";

    @Column(name = "category", nullable = false, length = 80)
    private String category = "";

    @Column(name = "label", nullable = false, length = 120)
    private String label = "";

    @Column(name = "description", nullable = false, type = "TEXT")
    private String description = "";

    @Column(name = "value_type", nullable = false, length = 24)
    private String valueType = WebSettingValueType.TEXT.name();

    @Column(name = "is_secret", nullable = false, defaultValue = "0")
    private int isSecret;

    @Column(name = "sort_order", nullable = false, defaultValue = "0")
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public int getId() { return id; }
    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public String getCategory() { return category; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getValueType() { return valueType; }
    public int getIsSecret() { return isSecret; }
    public int getSortOrder() { return sortOrder; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public void setCategory(String category) { this.category = category; }
    public void setLabel(String label) { this.label = label; }
    public void setDescription(String description) { this.description = description; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public void setIsSecret(int isSecret) { this.isSecret = isSecret; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
