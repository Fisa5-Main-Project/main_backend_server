package com.know_who_how.main_server.global.entity.Inheritance;

import jakarta.persistence.*;

import java.util.Date;

@Table(name="inheritance_recipients")
public class InheritanceRecipient {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "recipient_id")
    private Long recipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Long videoId;

    @Column(nullable = false)
    private String email;

    @Column(name="send_date", nullable=false)
    private Date sendDate;

    @Column(name = "link", nullable = false)
    private String link;

}
