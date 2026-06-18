package com.learn.projeto_learn.model.User;

public enum UserRole {
    ADMIN("admin"),
    MEDIC("medic"),
    RECEPCIONIST("recep"),
    PACIENTE("paciente");

    private final String role;

    UserRole(String role) { this.role = role; }

    public String getRole() { return role; }
}
