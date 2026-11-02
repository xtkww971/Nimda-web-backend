package com.nimda.cite.user.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = TableNames.authorityTable)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "authority_name", length = 20, unique = true, nullable = false)
    private String authorityName;
}
