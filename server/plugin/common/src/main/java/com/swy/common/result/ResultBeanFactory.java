package com.swy.common.result;

/**
 * 创建结果bean工厂类
 *
 * @author SkyWithYou
 */
public class ResultBeanFactory {
    
    /**
     * 创建成功结果
     *
     * @param <T> 数据泛型
     * @param data 返回数据
     * @param message 成功消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(T data, String message) {
        return ResultBean.success(data, message);
    }
    
    /**
     * 创建成功结果（无消息）
     *
     * @param <T> 数据泛型
     * @param data 返回数据
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(T data) {
        return ResultBean.success(data);
    }
    
    /**
     * 创建成功结果（无数据）
     *
     * @param <T> 数据泛型
     * @param message 成功消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> success(String message) {
        return ResultBean.success(message);
    }
    
    /**
     * 创建成功结果（无数据，默认消息）
     *
     * @param <T> 数据泛型
     * @return 结果对象
     */
    public static <T> ResultBean<T> success() {
        return ResultBean.success();
    }
    
    /**
     * 创建失败结果
     *
     * @param <T> 数据泛型
     * @param code 错误码
     * @param message 错误消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> fail(int code, String message) {
        return ResultBean.fail(code, message);
    }
    
    /**
     * 创建失败结果（默认错误码）
     *
     * @param <T> 数据泛型
     * @param message 错误消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> fail(String message) {
        return ResultBean.fail(message);
    }
    
    /**
     * 创建失败结果（默认错误码和消息）
     *
     * @param <T> 数据泛型
     * @return 结果对象
     */
    public static <T> ResultBean<T> fail() {
        return ResultBean.fail();
    }
    
    /**
     * 创建自定义结果
     *
     * @param <T> 数据泛型
     * @param data 返回数据
     * @param code 错误码
     * @param message 返回消息
     * @return 结果对象
     */
    public static <T> ResultBean<T> create(T data, int code, String message) {
        return new ResultBean<>(data, code, message);
    }
}
