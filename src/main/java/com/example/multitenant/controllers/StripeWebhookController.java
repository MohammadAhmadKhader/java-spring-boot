package com.example.multitenant.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.multitenant.config.StripeConfig;
import com.example.multitenant.models.enums.StripeEventType;
import com.example.multitenant.services.stripe.StripeHandlerService;
import com.example.multitenant.services.stripe.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {
    private final StripeConfig stripeConfig;
    private final StripeHandlerService stripeHandlerService;

    @PostMapping("")
    public ResponseEntity<Object> handleStripeEvent(@RequestBody byte[] payloadBytes, @RequestHeader("stripe-signature") String sigHeader) throws SignatureVerificationException, IOException {
        var payloadString = new String(payloadBytes, StandardCharsets.UTF_8);
        var event = Webhook.constructEvent(payloadString, sigHeader, stripeConfig.getWebhookSecret());
        var eventType = event.getType();
        log.info("received event: {}", eventType);

        if(eventType.equals(StripeEventType.CHECKOUT_SESSION_COMPLETED.getEvent())) {
            this.stripeHandlerService.handleCheckoutSessionCompletedEvent(event);
            
        } else if(eventType.equals(StripeEventType.CUSTOMER_SUBSCRIPTION_UPDATED.getEvent())) {
            this.stripeHandlerService.handleSubscriptionUpdate(event);

        } else if(eventType.equals(StripeEventType.CUSTOMER_SUBSCRIPTION_DELETED.getEvent())) {
            this.stripeHandlerService.handleSubscriptionCancellation(event);
            
        }

        return ResponseEntity.ok().build();
    }
}
