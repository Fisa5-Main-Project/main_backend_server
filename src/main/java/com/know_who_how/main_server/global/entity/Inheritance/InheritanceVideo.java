package com.know_who_how.main_server.global.entity.Inheritance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


@Entity
@Table(name="inheritance_videos")
@Getter
@Setter
@NoArgsConstructor
public class InheritanceVideo {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long videoId;

    @OneToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "inheritance_id", nullable = false, unique = true)
    private Inheritance inheritance;

    @Column(name="s3_object_key")
    private String s3ObjectKey;

    @Column(name="uploaded_at")
    private Date uploadedAt;

}
