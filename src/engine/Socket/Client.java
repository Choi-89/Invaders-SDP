package engine.Socket;

import screen.GameScreen;
import screen.MultiRoomScreen;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class Client {
    private String hostIp; // 서버의 실제 IP 주소
    private static char Button;
    private int port;
    private static int sleepTime = 250;
    public int moving;
    private static boolean checkConnect = false;

    public void connectServer(String hostIp) {
        this.hostIp = hostIp;// 서버 컴퓨터의 실제 IP 주소 입력
        this.port = 9000;
        System.out.println("Connecting to server at " + hostIp + ":" + port);
        String serverIp = "127.0.0.1"; // 서버 정보 전송용 서버 IP
        int infoPort = 9000; // 서버 정보 전송용 서버 포트
        GameScreen.setSORC(false);
        try (
                Socket infoSocket = new Socket(serverIp, infoPort);
                BufferedReader reader = new BufferedReader(new InputStreamReader(infoSocket.getInputStream()))
        ) {
            // 서버 정보 수신
            String receivedIp = reader.readLine();
            System.out.println(receivedIp);
            int receivedPort = Integer.parseInt(reader.readLine());
            System.out.println("Received server info: IP=" + receivedIp + ", Port=" + receivedPort);
            checkConnect = true;
            // 통신용 서버에 연결
            connectToMainServer(receivedIp, receivedPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void connectToMainServer(String serverIp, int serverPort) {
        try (
                Socket mainSocket = new Socket(serverIp, serverPort);
                BufferedReader reader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(mainSocket.getOutputStream(), true);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to server at " + serverIp + ":" + serverPort);
            writer.println(10);
            // 수신 스레드
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println("Server: " + message);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost from server.");
                }
            });

            // 송신 스레드
            Thread sendThread = new Thread(() -> {
                String message;
                while ((message = String.valueOf(Button)) != null) {
                    writer.println(message);
                    System.out.println("You: " + message);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // 스레드 시작
            receiveThread.start();
            sendThread.start();

            // 두 스레드가 모두 종료될 때까지 대기
            receiveThread.join();
            sendThread.join();
            Thread.sleep(10000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setButton(char button) {
        this.Button = button;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static boolean checkConnect() {
        return checkConnect;
    }
}
