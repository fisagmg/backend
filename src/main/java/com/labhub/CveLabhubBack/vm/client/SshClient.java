package com.labhub.CveLabhubBack.vm.client;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.labhub.CveLabhubBack.vm.config.TerraformConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class SshClient {

    private final TerraformConfig terraformConfig;

    /**
     * SSH로 원격 명령어 실행
     */
    public String executeCommand(String command) throws Exception {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            // SSH 세션 생성
            session = jsch.getSession(
                    terraformConfig.getUser(),
                    terraformConfig.getHost(),
                    terraformConfig.getPort()
            );
            session.setPassword(terraformConfig.getPassword());

            // Host Key 체크 비활성화
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // 연결
            session.connect(10000); // 10초 타임아웃
            log.info("SSH 연결 성공: {}@{}", terraformConfig.getUser(), terraformConfig.getHost());

            // 명령어 실행 채널 열기
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // 출력 스트림 설정
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();

            channel.connect();

            // 표준 출력 읽기
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 에러 출력 읽기
            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            // Exit 코드 확인
            int exitCode = channel.getExitStatus();
            log.info("명령어 실행 완료. Exit Code: {}", exitCode);

            if (exitCode != 0 && errorOutput.length() > 0) {
                log.error("명령어 실행 에러: {}", errorOutput.toString());
                throw new RuntimeException("SSH 명령어 실행 실패: " + errorOutput.toString());
            }

            return output.toString();

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}