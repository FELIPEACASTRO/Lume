package com.lume.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "shell_navigation_items")
public class ShellNavigationItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "path", nullable = false, length = 255)
    private String path;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "icon", nullable = false, length = 64)
    private String icon;

    @Column(name = "availability", nullable = false, length = 32)
    private String availability;

    @Column(name = "nav_group", nullable = false, length = 32)
    private String navGroup;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "keywords_raw", nullable = false, columnDefinition = "TEXT")
    private String keywordsRaw = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getNavGroup() {
        return navGroup;
    }

    public void setNavGroup(String navGroup) {
        this.navGroup = navGroup;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeywordsRaw() {
        return keywordsRaw;
    }

    public void setKeywordsRaw(String keywordsRaw) {
        this.keywordsRaw = keywordsRaw;
    }
}
