package com.vivi.gulimall.thirdparty.service;


import com.vivi.common.to.SmsSendCodeTO;

/**
 * @author  
 * 2021/1/13 13:17
 */
public interface SmsService {

    void sendCode(SmsSendCodeTO smsSendCodeTO);
}
