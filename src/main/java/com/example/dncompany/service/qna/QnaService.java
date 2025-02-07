package com.example.dncompany.service.qna;

import com.example.dncompany.dto.page.PageDTO;
import com.example.dncompany.dto.page.PageRequestDTO;
import com.example.dncompany.dto.qna.QnADTO;
import com.example.dncompany.dto.qna.QnADetailDTO;
import com.example.dncompany.dto.qna.QnAModifyDTO;
import com.example.dncompany.dto.qna.QnAWriteDTO;
import com.example.dncompany.dto.qna.qnaPage.QnaBoardSearchDTO;
import com.example.dncompany.exception.qna.QnANotFoundException;
import com.example.dncompany.mapper.qna.QnaMapper;
import com.example.dncompany.mapper.zip.ZipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QnaService {
    private final QnaMapper qnaMapper;

    // 게시글 전체 정보
    public List<QnADTO> getAllQnaBoards() {
        return qnaMapper.selectAllQnABoards();
    }

    //
    public QnADetailDTO getQnAById(Long qnaId) {
        return qnaMapper.QnASelectById(qnaId)
                .orElseThrow(() -> new QnANotFoundException("게시글을 찾을 수 없음, iD : " + qnaId));
    }

    // 게시글 삽입
    public void addQnaBoard(QnAWriteDTO qnaWriteDTO, Long usersId) {
        qnaWriteDTO.setUsersId(usersId);
        qnaMapper.insertQnABoard(qnaWriteDTO);
    }

    // 게시글 수정
    public void modifyQnaBoard(QnAModifyDTO qnaModifyDTO) {
        log.info("모디피로ㅓ그다"+qnaModifyDTO.toString());
        qnaMapper.updateQnABoard(qnaModifyDTO);
    }

    //게시글 삭제
    public void removeQnABoard(Long qnaId) {
        qnaMapper.deleteQnABoard(qnaId);
    }

    // 페이징 처리
    public PageDTO<QnADTO> getQnaBoardsBySearchCondWithPage (QnaBoardSearchDTO qnaBoardSearchDTO,
                                                             PageRequestDTO pageRequestDTO) {

        List<QnADTO> qnaList = qnaMapper.selectBySearchCondWithPage(qnaBoardSearchDTO, pageRequestDTO);
        int total = qnaMapper.countBySearchCondition(qnaBoardSearchDTO);

        return new PageDTO<>(pageRequestDTO.getPage(),
                pageRequestDTO.getSize(),
                total,
                qnaList);
    }
}
