package com.wind.oauth.integration.other;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.wind.mybatis.pojo.User;
import com.wind.oauth.integration.IntegrationAuthentication;
import com.wind.oauth.integration.IntegrationAuthenticator;
import com.wind.web.service.UserService;
import lombok.extern.apachecommons.CommonsLog;
import me.chanjar.weixin.common.exception.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@CommonsLog
@Component
public class MiniAppAuthenticator extends IntegrationAuthenticator {

    @Autowired
    private UserService userService;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean support(IntegrationAuthentication integrationAuthentication) {
        return "wx".equals(integrationAuthentication.getAuthType());
    }

    public User authenticate(IntegrationAuthentication integrationAuthentication) {
        WxMaJscode2SessionResult session;
        String code = integrationAuthentication.getAuthParameter("password");
        try {
            session = this.wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            e.printStackTrace();
            throw new InternalAuthenticationServiceException("获取微信小程序用户信息失败", e);
        }
        log.info(session);
        String openId = session.getOpenid();
        String sessionKey = session.getSessionKey();
        String unionid = session.getUnionid();
        Optional<User> user = userService.selectByOpenId(openId);
        if (user.isPresent()) {
            User result = user.get();
            result.setPassword(passwordEncoder.encode(code));
            return result;
        } else {
            User newUser = new User();
            newUser.setOpenId(openId);
            newUser.setEnabled(true);
            newUser.setAuthority("user");
            String password = passwordEncoder.encode(code);
            newUser.setPassword(password);
            newUser.setSessionKey(sessionKey);
            newUser.setUnionId(unionid);
            userService.add(newUser);
            User result = new User();
            result.setPassword(password);
            result.setEnabled(true);
            result.setAuthority("user");
            return result;
        }
    }
}