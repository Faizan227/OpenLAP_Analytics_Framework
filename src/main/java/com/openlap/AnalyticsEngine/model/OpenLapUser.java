package com.openlap.AnalyticsEngine.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
public class OpenLapUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Type(type = "objectid")
    String id;

    String email;

    String password;

    String confirmpassword;

    String firstname;

    String lastname;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private List<Roles> roles;

    //Set roles;

    public OpenLapUser() {
    }

    public OpenLapUser(String email, String password, String confirmpassword, String firstname, String lastname) {
        this.email = email;
        this.password = password;
        this.confirmpassword = confirmpassword;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getConfirmpassword() {
        return confirmpassword;
    }

    public void setConfirmpassword(String confirmpassword) {
        this.confirmpassword = confirmpassword;
    }

    public List<Roles> getRoles() {
        return roles;
    }

    public void setRoles(List<Roles> roles) {
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenLapUser that = (OpenLapUser) o;
        return id.equals(that.id) &&
                email.equals(that.email) &&
                password.equals(that.password) &&
                confirmpassword.equals(that.confirmpassword) &&
                firstname.equals(that.firstname) &&
                lastname.equals(that.lastname) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, password, confirmpassword, firstname, lastname);
    }

    @Override
    public String toString() {
        return "OpenLapUser{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", confirmpassword='" + confirmpassword + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                '}';
    }
}
