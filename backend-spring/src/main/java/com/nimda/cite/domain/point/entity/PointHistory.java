package com.nimda.cite.domain.point.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import lombok.Getter;

@Table(name = TableNames.pointHistoryTable)
@Entity
@Getter
public class PointHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "point_detail_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private PointDetail pointDetail;
}
