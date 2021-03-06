package com.github.lyd.msg.provider.service.impl;


import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.github.lyd.msg.client.dto.sms.SmsParameter;
import com.github.lyd.msg.client.dto.sms.SmsSendResult;
import com.github.lyd.msg.client.dto.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * @author woodev
 */
@Slf4j
public class AliyunSmsSenderImpl implements SmsSender {

    private String accessKeyId;

    private String accessKeySecret;

    private String STATUS_OK = "OK";

    public AliyunSmsSenderImpl(){
        log.info("初始化阿里云接口:" + this);
    }

    @Override
    public SmsSendResult send(SmsParameter parameter) {
        SmsSendResult result = new SmsSendResult();
        result.setSuccess(true);

        DefaultProfile profile = DefaultProfile.getProfile(
                "cn-guangzhou",          // 地域ID
                accessKeyId,
                accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        //request.setProtocol(ProtocolType.HTTPS);
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", StringUtils.join(parameter.getPhoneNumbers(),","));
        request.putQueryParameter("SignName", parameter.getSignName());
        request.putQueryParameter("TemplateCode", parameter.getTemplateCode());
        request.putQueryParameter("TemplateParam", parameter.getParams());

        CommonResponse response = null;
        try {
            response = client.getCommonResponse(request);
        } catch (ServerException e) {
            log.error("发送短信失败：" + e.getMessage(), e);
            result.setSuccess(false);
            result.setCode("SEND_SMS_FAILURE");
            throw new RuntimeException("发送短信发生错误：" + e);
        } catch (ClientException e) {
            log.error("发送短信失败：" + e.getMessage(), e);
            result.setSuccess(false);
            result.setCode("SEND_SMS_FAILURE");
            throw new RuntimeException("发送短信发生错误：" + e);
        }

        return result;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

}
