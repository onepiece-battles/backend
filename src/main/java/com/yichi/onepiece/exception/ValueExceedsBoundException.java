package com.yichi.onepiece.exception;

public class ValueExceedsBoundException extends RuntimeException{
    public ValueExceedsBoundException() {
        super();
    }

    public ValueExceedsBoundException(String s) {
        super(s);
    }

    public ValueExceedsBoundException(Throwable cause) {
        super();
    }
}
