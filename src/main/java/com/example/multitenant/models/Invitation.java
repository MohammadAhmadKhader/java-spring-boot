package com.example.multitenant.models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.CreationTimestamp;

import com.example.multitenant.dtos.invitations.InvitationViewDTO;
import com.example.multitenant.models.enums.InvitationStatus;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invitations", indexes = {
    @Index(name ="idx_invitation_organization_id", columnList = "organization_id"),
    @Index(name ="idx_invitation_sender_id", columnList = "sender_id"),
    @Index(name ="idx_invitation_recipient_id", columnList = "recipient_id")
})
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "organization_id", insertable = false, updatable = false)
    private Integer organizationId;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "sender_id", insertable = false, updatable = false)
    private Long senderId;

    @Column(name = "recipient_id", insertable = false, updatable = false)
    private Long recipientId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column
    private InvitationStatus status;

    @CreationTimestamp
    private Instant createdAt;

    @PrePersist
    public void loadDefaults() {
        this.status = InvitationStatus.PENDING;
    }

    public InvitationViewDTO toViewDTO() {
        return new InvitationViewDTO(this);
    }
}
