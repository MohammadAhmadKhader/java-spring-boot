package com.example.multitenant.services.contents;

import org.springframework.stereotype.Service;

import com.example.multitenant.models.Content;
import com.example.multitenant.repository.ContentsRepository;
import com.example.multitenant.services.cache.RedisService;
import com.example.multitenant.services.ownership.impl.OwnershipServiceImpl;

@Service
public class ContentsOwnershipService extends OwnershipServiceImpl<Content, Integer, ContentsRepository> {
    
    public ContentsOwnershipService(ContentsRepository contentsRepository, RedisService redisService) {
        super(contentsRepository, redisService, () -> {
            return new Content();
        });
    }
}