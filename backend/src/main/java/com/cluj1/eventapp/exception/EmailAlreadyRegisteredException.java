package com.cluj1.eventapp.exception;

public class EmailAlreadyRegisteredException extends RuntimeException{

    public EmailAlreadyRegisteredException(){
        super("There is already an account registered to this email address!");
    }
}
