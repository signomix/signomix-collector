package com.signomix.collector.app.ports.driving;

import org.jboss.logging.Logger;

import com.signomix.collector.app.logic.AuthLogic;
import com.signomix.common.Token;
import com.signomix.common.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthPort {

    @Inject
    Logger logger;

    @Inject
    AuthLogic authLogic;

    public String getUserId(String token){
        if(token!=null && token.endsWith("/")){
            token=token.substring(0,token.length()-1);
        }
        logger.info("getUserId: "+token);
        Token t=authLogic.getToken(token);
        if(t!=null){
            if(t.getIssuer()!=null && !t.getIssuer().isEmpty()){
                return t.getIssuer();
            }else{
                return t.getUid();
            }
        }else{
            return null;
        }
    }

    /**
     * Updates token lifetime and returns user id
     * @param token
     * @return
     */
    public String updateUserId(String token){
        if(token!=null && token.endsWith("/")){
            token=token.substring(0,token.length()-1);
        }
        logger.debug("getUserId: "+token);
        Token t=authLogic.updateToken(token);
        if(t!=null){
            if(t.getIssuer()!=null && !t.getIssuer().isEmpty()){
                return t.getIssuer();
            }else{
                return t.getUid();
            }
        }else{
            return null;
        }
    }

    public User getUser(String token){
        return authLogic.getUser(getUserId(token));
    }
}
