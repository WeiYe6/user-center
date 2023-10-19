package com.yupi.usercenter.common;

/**
 * 返回工具类
 *
 * @author 曾伟业
 */
public class ResultUtils {
    /**
     * 成功
     * @param data
     * @return
     * @param <T>
     *
     *    第一个 T 表示方法的返回类型是一个泛型类型参数，
     *    第二个 T 表示 BaseResponse类的泛型类型参数 与方法的泛型类型参数一致，
     *    第三个 T 表示方法的参数 data 的类型与方法的泛型类型参数一致。
     *    --通过这样的泛型定义和使用，可以使方法具有更好的通用性和灵活性
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, data, "ok");
    }
    /**
     * 失败
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }
    /**
     * 失败
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static <T> BaseResponse<T> error(int code, String message, String description){
        return new BaseResponse<>(code, null, message, description);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @param message
     * @param description
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message, String description){
        return new BaseResponse<>(errorCode.getCode(), null, message, description);
    }

    /**
     * 失败
     * @param errorCode
     * @param description
     * @return
     */
    public static <T> BaseResponse error(ErrorCode errorCode, String description){
        return new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), description);
    }
}
