package com.spring.exception;

/**
 * Spring Bean相关异常的基类
 */
public class BeansException extends RuntimeException {

    /**
     * 创建一个新的BeansException实例
     *
     * @param message 异常信息
     */
    public BeansException(String message) {
        super(message);
    }

    /**
     * 创建一个新的BeansException实例
     *
     * @param message 异常信息
     * @param cause 异常原因
     */
    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }
} 