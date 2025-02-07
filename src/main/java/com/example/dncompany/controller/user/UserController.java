package com.example.dncompany.controller.user;

import com.example.dncompany.dto.user.UserJoinDTO;
import com.example.dncompany.dto.user.UserJoinKakaoDTO;
import com.example.dncompany.dto.user.UserLoginDTO;
import com.example.dncompany.dto.user.UserSessionDTO;
import com.example.dncompany.exception.user.LoginFailedException;
import com.example.dncompany.exception.user.UserDuplicateException;
import com.example.dncompany.service.user.AuthService;
import com.example.dncompany.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/user/signup")
    public String signup() {
        return "user/signup";
    }

    @PostMapping("/user/signup")
    public String join(UserJoinDTO userJoinDTO) {
        log.debug("userJoinDTO: {}", userJoinDTO);
        log.info("userJoinDTO: {}", userJoinDTO);

        try {
            userService.addUser(userJoinDTO);
            return "redirect:/user/login";
        } catch (UserDuplicateException e) {
            log.error(e.getMessage());
            return "redirect:/user/signup";
        }
    }

    @GetMapping("/user/login")
    public String login(@RequestParam(defaultValue = "false") boolean hasError,
                        Model model) {
        model.addAttribute("hasError", hasError);
        return "user/login";
    }

    @PostMapping("/user/login")
    public String login(UserLoginDTO userLoginDTO,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            UserSessionDTO loginInfo = userService.getLoginInfo(userLoginDTO);

            log.info("UserStatus: {}, BanEndDate: {}, CurrentTime: {}",
                    loginInfo.getUserStatus(), loginInfo.getBanEndDate(), new Date());

            /**(제현)
             * 서비스로 옴겨 같음 정지일때도 모두 아이디 비번 오류로 처리함
             * 애초에 쿼리가 잘 못 됬음 쿼리에서 조건을 시스데이트보다 정지 만료일이
             * 작은 데이터만 뽑으라니까 정지인 사람은 애초에 뽑지 못하고 null이 반환됨
             * 그래서 로그인은 불가능하지만 정확한 비교가 불가능했음
             * 아래 쿼리를 수정 후 검사 로직을 서비스로 옴김
             */
            // 정지 상태 확인 로직
//            if ("SUSPENDED".equals(loginInfo.getUserStatus())) {
//                log.warn("계정 정지 상태 - 정지 해제 시간: {}", loginInfo.getBanEndDate());
//                redirectAttributes.addFlashAttribute("message",
//                        "계정이 정지되었습니다. 정지 해제 날짜: " + loginInfo.getBanEndDate());
//                // 정지 해제 날짜 확인
//                if (loginInfo.getBanEndDate() != null) {
//                    redirectAttributes.addFlashAttribute("message",
//                            "계정이 정지되었습니다. 정지 해제 날짜: " + loginInfo.getBanEndDate());
//                    return "redirect:/user/login";
//                }
//            }


            session.setAttribute("usersId", loginInfo.getUsersId());
            session.setAttribute("loginId", loginInfo.getLoginId());
            session.setAttribute("role", loginInfo.getRole());

            log.info("userLoginDTO: {}", userLoginDTO);
            log.debug("userLoginDTO: {}", userLoginDTO);

            return "redirect:/";
        } catch (LoginFailedException e) {
            log.error(e.getMessage());
            log.error("로그인 실패: {}", e.getMessage());

            redirectAttributes.addAttribute("hasError", true);

            redirectAttributes.addFlashAttribute("message", e.getMessage());

            return "redirect:/user/login";
        }
    }

    //    카카오 인증
    @GetMapping("/user/auth/kakao/login")
    public String kakaoLogin() {
        System.out.println("User.kakaoLogin");

        String location = authService.getKakaoLoginURI();
        return "redirect:" + location;

    }

    @GetMapping("/auth/kakao/callback")
    public String kakaoCallback(String code, HttpSession session, RedirectAttributes redirectAttributes) {
        System.out.println("code = " + code);

        // 카카오 로그인 정보를 이용해 카카오 ID를 가져옴
        Long kakaoId = authService.getKakaoLoginInfo(code);
        System.out.println("kakaoId = " + kakaoId); // 카카오 ID 로그로 확인

        try {
            // 카카오 ID로 이미 등록된 사용자가 있는지 확인

            UserSessionDTO userSessionDTO = userService.addKakaoUser(kakaoId);

            session.setAttribute("usersId", userSessionDTO.getUsersId());
            session.setAttribute("loginId", userSessionDTO.getLoginId());
            session.setAttribute("role", userSessionDTO.getRole());


            return "redirect:/"; // 로그인 후 홈으로 리디렉션

        } catch (Exception e) {
            // 로그인 정보가 없을 경우 (예상하지 못한 오류)
            // 예외 발생 시 로그 출력 및 회원가입 페이지로 리디렉션
            System.out.println("에러 발생: " + e.getMessage());
            e.printStackTrace(); // 예외 상세 출력
            UserJoinKakaoDTO userJoinKakaoDTO = new UserJoinKakaoDTO();
            userJoinKakaoDTO.setKakaoId(kakaoId);

            userService.addKakaoIdUsers(userJoinKakaoDTO);

            Long usersId = userJoinKakaoDTO.getUsersId();
            redirectAttributes.addFlashAttribute("usersId", usersId);
            return "redirect:/user/update/kakao"; // 에러 발생 시 회원가입 페이지로 리디렉션
        }
    }

    @PostMapping("/user/update/kakao")
    public String kakaoLoginUpdate(){
        return "user/kakaoupdate";
    }

    //      유저 테이블에 카카오 인증 칼럼을 추가하고
//      카카오 인증으로 회원가입하면 칼럼에 인증했다는 기록을 남기고
//      이후 회원가입 창으로 이동시켜 정보를 입력받고
//      이 정보를 DB에 담은 다음에
//      이후 카카오 로그인을 했을 때는 DB에 입력 되어 있는 정보를 받아서
//      로그인 처리를 완료하는 방법이 가능한가.

    @GetMapping("/user/logout")
    public String logout(HttpSession session) {
        session.invalidate();

        return "redirect:/";
    }


}
