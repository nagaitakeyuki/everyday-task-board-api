package io.taskboard.app.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUserForm implements Serializable {
    private String email;
    private String userName;
    private String pass;
}
