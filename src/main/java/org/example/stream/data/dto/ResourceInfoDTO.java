package org.example.stream.data.dto;

import java.io.Serializable;

/**
 * Author: wanghao
 * Date: 2024/4/3 14:10
 * Description:
 */
public class ResourceInfoDTO implements Serializable {

    private String name;

    /**
     * 资源id
     */
    private Long resourceId;

    /**
     * 资源类型
     */
    private Integer type;

    /**
     * 设备号
     */
    private String deviceId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 阶段：1 -【6:00-10:00为早高峰】，2 -【10：00-16:00为午间】，3-【16:00-20:00位晚高峰】，4-【20:00-06:00为晚间】
     */
    private Integer stage;

    /**
     * 行为：1-收听，2-订阅，3-取消订阅
     */
    private Integer act;

    /**
     * 登录状态 1-登录，2-未登录
     */
    private Integer loginStatus;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getAct() {
        return act;
    }

    public void setAct(Integer act) {
        this.act = act;
    }

    public Integer getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(Integer loginStatus) {
        this.loginStatus = loginStatus;
    }
}
