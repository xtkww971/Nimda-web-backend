package com.nimda.cite.domain.attendance.entity;

import com.nimda.cite.common.util.TableNames;
import com.nimda.cite.user.entity.User;
import jakarta.persistence.*;
        import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Entity
@Getter
@Table(name = TableNames.attendanceLogTable)
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 혹은 attendance_id
    private User user;

    @Column(name = "attendance_date")
    private LocalDateTime attendanceDate;
}