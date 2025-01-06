package com.example.dncompany.dto.admin.board;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class AdminEventBoardDTO {
    private Long eventId;
    private String category;
    private String title;
    private String status;
    private String writer;
    private String email;
    private String phoneNumber;
    private String createdAt;
}
