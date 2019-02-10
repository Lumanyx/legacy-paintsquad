package de.xenyria.splatoon.game.objects;

public class InvalidObjectConfigurationException extends Exception {

    private String finalMsg;

    public InvalidObjectConfigurationException(ObjectType type, String msg) {

        this.finalMsg = "[" + type.name() + "] " + msg;

    }

}
