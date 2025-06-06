package com.example.multitenant.services.channels;

import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.example.multitenant.dtos.categories.CategoryOrderSwapDTO;
import com.example.multitenant.dtos.channels.ChannelOrderSwapDTO;
import com.example.multitenant.exceptions.ResourceNotFoundException;
import com.example.multitenant.exceptions.UnauthorizedUserException;
import com.example.multitenant.models.Channel;
import com.example.multitenant.repository.ChannelsRepository;
import com.example.multitenant.services.organizations.OrgsService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChannelsService {
    private final ChannelsRepository channelsRepository;
    
    public List<Channel> findAll(Integer organizationId) {
        return this.channelsRepository.findAllByOrgId(organizationId);
    }
    
    public Channel findByIdAndOrganizationId(Integer id, Integer organizationId, Integer categoryId) {
        var chan = this.channelsRepository.findByIdAndOrgId(id, organizationId);
        if(chan == null) {
            throw new ResourceNotFoundException("channel", id);
        }

        if(!chan.getCategoryId().equals(categoryId)) {
            throw new UnauthorizedUserException("channel", id);
        }
        
        return chan;
    }

    public Channel findWithMessagesByIdAndOrganizationId(Integer id, Integer organizationId, Integer categoryId) {
        var chan = this.channelsRepository.findWithMessagesByIdAndOrgId(id, organizationId);
        if(chan == null) {
            throw new ResourceNotFoundException("channel", id);
        }
        
        if(!chan.getCategoryId().equals(categoryId)) {
            throw new UnauthorizedUserException("channel", id);
        }
        
        return chan;
    }
    
    public Channel create(Channel channel, Integer organizationId, Integer categoryId) {
        channel.setOrganizationId(organizationId);
        channel.setCategoryId(categoryId);
        var latestOrderCategory = this.channelsRepository.findLatestOrder(organizationId, categoryId);
        
        Integer displayOrder;
        if(latestOrderCategory == null) {
            displayOrder = 1;
        } else {
            displayOrder = latestOrderCategory.getDisplayOrder() + 1;
        }

        channel.setDisplayOrder(displayOrder);
        return this.channelsRepository.saveAndFlush(channel);
    }
    
    public Channel update(Integer id, Channel updatedChannel, Integer organizationId, Integer categoryId) {
        var channel = this.channelsRepository.findByIdAndOrgId(id, organizationId);
        if(updatedChannel.getName() != null) {
            channel.setName(updatedChannel.getName());
        }

        if(!channel.getCategoryId().equals(categoryId)) {
            throw new UnauthorizedUserException("channel", id);
        }
        
        if(updatedChannel.getDisplayOrder() != null) {
            channel.setDisplayOrder(updatedChannel.getDisplayOrder());
        }
        
        return this.channelsRepository.save(channel);
    }
    
    public void delete(Integer id, Integer organizationId, Integer categoryId) {
        this.channelsRepository.deleteByIdAndOrgId(id, organizationId, categoryId);
    }

    @Transactional
    public void swapChannelOrder(ChannelOrderSwapDTO dto, Integer orgId, Integer categoryId) {
        var chan1 = this.channelsRepository.findByIdAndOrgIdAndCatIdLocked(dto.getChannelId1(), orgId, categoryId);
        if(chan1 == null) {
            throw new ResourceNotFoundException("channel", dto.getChannelId1());
        }

        var chan2 = this.channelsRepository.findByIdAndOrgIdAndCatIdLocked(dto.getChannelId2(), orgId, categoryId);
        if (chan2 == null) {
            throw new ResourceNotFoundException("channel", dto.getChannelId2());
        }

        var tempOrder = chan1.getDisplayOrder();
        chan1.setDisplayOrder(chan2.getDisplayOrder());
        chan2.setDisplayOrder(tempOrder);

        this.channelsRepository.saveAll(List.of(chan1, chan2));
    }

    public Channel findOne(Integer channelId, Integer organizationId, Integer categotyId) {
        var probe = new Channel();
        probe.setId(channelId);
        probe.setOrganizationId(organizationId);
        probe.setCategoryId(categotyId);

        return this.channelsRepository.findOne(Example.of(probe)).orElse(null);
    }

    public long countChannelsByCategoryId(Integer categoryId, Integer orgId) {
        return this.channelsRepository.countChannelsByCategoryId(categoryId, orgId);
    }

    public long countChannelsByOrgId(Integer orgId) {
        return this.channelsRepository.countChannelsByOrgId(orgId);
    }
}
