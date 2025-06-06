package com.example.multitenant.controllers;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.example.multitenant.common.annotations.contract.CheckRestricted;
import com.example.multitenant.dtos.auth.UserPrincipal;
import com.example.multitenant.dtos.conversationmessages.ConversationMessageCreateDTO;
import com.example.multitenant.dtos.messages.*;
import com.example.multitenant.exceptions.UnauthorizedUserException;
import com.example.multitenant.models.User;
import com.example.multitenant.services.conversations.ConversationsService;
import com.example.multitenant.services.messages.OrgMessagesService;
import com.example.multitenant.services.users.UsersService;
import com.example.multitenant.services.websocket.WebSocketService;
import com.example.multitenant.utils.AppUtils;
import com.example.multitenant.utils.SecurityUtils;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WebSocketMessagesController {
    private final OrgMessagesService orgMessagesService;
    private final WebSocketService webSocketService;
    private final ConversationsService conversationsService;

    @CheckRestricted(isWebsocket = true)
    @MessageMapping("/tenants/{tenantId}/categories/{categoryId}/channels/{channelId}/send")
    public void handleSendMessageToChannel(@Payload @Validated OrgMessageCreateDTO payload,
        @DestinationVariable Integer tenantId, @DestinationVariable Integer channelId, 
        @DestinationVariable Integer categoryId, Principal principal) {
            
        var user = SecurityUtils.getUserFromPrincipal(principal);
        if(user != null) {
            log.info("received message {}", payload.getContent());
            log.info("principal {}", user.getFirstName());

            var message = payload.toModel();
            message.setSender(user);

            var createdMsg = this.orgMessagesService.create(message, channelId, tenantId);
            this.webSocketService.publishNewOrgMessage(createdMsg, tenantId, categoryId);
            
        } else {
            log.error("user was not found during attempt to fetch it from principal");
            throw new UnauthorizedUserException("unauthorized");
        }
    }

    @CheckRestricted(isWebsocket = true)
    @MessageMapping("/conversations/{conversationId}")
    public void handleSendMessageToUser(@Payload @Validated ConversationMessageCreateDTO payload,
        @DestinationVariable Integer conversationId, Principal principal) {
            
        var user = SecurityUtils.getUserFromPrincipal(principal);
        if(user != null) {
            log.info("received message {}", payload.getContent());
            log.info("principal {}", user.getFirstName());

            var message = payload.toModel();
            message.setSender(user);

            var conv = this.conversationsService.addMessageToConv(user ,conversationId, message);
            var target = AppUtils.getWsTarget(user, conv);

            this.webSocketService.publishNewConvMessage(conv.getLastMessage(), target);
            
        } else {
            log.error("user was not found during attempt to fetch it from principal");
            throw new UnauthorizedUserException("unauthorized");
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        log.warn("websocket error occurred: {}", exception.getMessage());
        return exception.getMessage();
    }
}
