package com.swy.common.result;

import lombok.Data;

/**
 * 通用结果封装类
 *
 * @author swy
 * @param <T> 数据泛型
 */

 @Data
public class ResultBean<T> {

    /**
     * 返回数据
     */
    private T data;

    /**
     * 错误码
     */
    private int code;
    
    /**
     * 返回消息
     */
    private String message;
    
    /**
     * 默认构造函数
     */
    public ResultBean() {
    }
    
    /**
     * 构造函数
     *
     * @param data 返回数据
     * @param code 错误码
     * @param message 返回消息
     */
    public ResultBean(T data, int code, String message) {
        this.data = data;
        this.code = code;
        this.message = message;
    }
    
    /**
     * 成功结果构造函数
     *
     * @param data 返回数据
     * @param message 返回消息
     */
    public ResultBean(T data, String message) {
        this(data, 0, message);
    }
    
    /**
     * 失败结果构造函数
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public ResultBean(int code, String message) {
        this(null, code, message);
    }
    
    /**
     * 创建成功结果
     *
     * @param <T> 数据泛型
     * @param data 返回数据
     * @param message 成功消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(T data, String message) {
        return new ResultBean<>(data, 0, message);
    }
    
    /**
     * 创建成功结果（无消息）
     *
     * @param <T> 数据泛型
     * @param data 返回数据
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(T data) {
        return success(data, "操作成功");
    }
    
    /**
     * 创建成功结果（无数据）
     *
     * @param <T> 数据泛型
     * @param message 成功消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(String message) {
        return success(null, message);
    }
    
    /**
     * 创建成功结果（无数据，默认消息）
     *
     * @param <T> 数据泛型
     * @return 结果对象
     */
    public static <T> ResultBean<T> success() {
        return success(null, "操作成功");
    }
    
    /**
     * 创建失败结果
     *
     * @param <T> 数据泛型
     * @param code 错误码
     * @param message 错误消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> error(int code, String message) {
        return new ResultBean<>(null, code, message);
    }
    
    /**
     * 创建失败结果（默认错误码）
     *
     * @param <T> 数据泛型
     * @param message 错误消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> error(String message) {
        return error(500, message);
    }
    
    /**
     * 创建失败结果（默认错误码和消息）
     *
     * @param <T> 数据泛型
     * @return 结果对象
     */
    public static <T> ResultBean<T> error() {
        return error(500, "操作失败");
    }

    /**
     * 是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code == 0;
    }
}