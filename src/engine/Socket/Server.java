package engine.Socket;

import engine.Core;
import engine.ServerManager;
import entity.Room;
import screen.GameScreen;
import screen.MultiRoomScreen;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
// !!!!!!!!!아이피 고정으로 바꾸기!!!!!!!!!!
public class Server {
    private static ServerManager serverManager;
    private static Client client;
    private static String hostIp; //
    private static final int INFO_PORT = 9000;
    private static int MAIN_PORT = 9001;
    private static int sleepTime = 50;
    private static String Button; // 초기화
    private static boolean checkConnect = false;
    private static int returnCode;
    private static ServerSocket infoServerSocket = null;
    public static String takeButton;
    private static List<String> giveShooter;

    public Server(ServerManager serverManager, Client client) {
        Server.serverManager = serverManager;
        Server.client = client;
        serverManager.setServer(this);
    }

    // 서버 정보 전송용 서버
    public static void startInfoServer() {
        new Thread(() -> {
            try{
                infoServerSocket = new ServerSocket(INFO_PORT, 50, InetAddress.getByName(hostIp));
                System.out.println("Info server running on port " + INFO_PORT);
                GameScreen.setSORC(true);
                while (true) {
                        Socket socket = infoServerSocket.accept();
                        System.out.println("Client connected to info server: " + socket.getInetAddress());
                        // 서버 IP와 포트 정보 전송
                        sendServerInfo(socket);
                }
            } catch (IOException e) {
                System.out.println("An exception occurred: " + e.getMessage());
                if (infoServerSocket != null && !infoServerSocket.isClosed()) {
                    try {
                        infoServerSocket.close(); // catch 블록에서 서버 소켓을 닫기
                    } catch (IOException ex) {
                        System.out.println("Failed to close server socket: " + ex.getMessage());
                    }
                }
            }
        }).start();
    }

    // 클라이언트와의 통신용 서버
    public static void startMainServer() {
        new Thread(() -> {
            try (ServerSocket mainServerSocket = new ServerSocket(MAIN_PORT, 50, InetAddress.getByName(hostIp))) {
                System.out.println("Main server running on port " + MAIN_PORT);
                while (true) {
                    try {
                        mainServerSocket.setSoTimeout(3000);
                        Socket clientSocket = mainServerSocket.accept();
                        System.out.println("Client connected to main server: " + clientSocket.getInetAddress());
                        checkConnect = true;
                        // 클라이언트와 통신 처리
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (e.getMessage().equals("Accept timed out")) {
                            break;
                        }
                    }

                }
                System.out.println("Main server stopped");
                mainServerSocket.close();
                MultiRoomScreen.getErrorCheck(1);
            } catch (IOException e) {
                e.printStackTrace();
                if(e.getMessage().equals("Address already in use")) {
                    System.out.println("go to client");
                    MultiRoomScreen.getErrorCheck(2);
                    client.connectServer(hostIp);
                }else{
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 서버 IP와 포트 정보를 클라이언트로 전송
    private static void sendServerInfo(Socket clientSocket) {
        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String serverIp = hostIp;
            writer.println(serverIp);
            writer.println(MAIN_PORT); // 통신용 서버 포트
            System.out.println("Sent server info: IP=" + serverIp + ", Port=" + MAIN_PORT);
        } catch (IOException e) {
            System.out.println("Error sending server info: " + e.getMessage());
            MultiRoomScreen.getErrorCheck(1);
        }
    }

    //클라이언트 처리
    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
        ) {
            int t = Integer.parseInt(reader.readLine());
            returnCode = t;

            // 수신 스레드
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {//(message = reader.readLine()) != null
                        takeButton = reader.readLine();
                        serverManager.setServerButton(takeButton);
                        System.out.println("Client: " + takeButton);
                        try {
                            takeButton = null;
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost: " + clientSocket.getInetAddress());
                }
            });

            // 송신 스레드
            Thread sendThread = new Thread(() -> {
                String message;
                while (true) {//(message = String.valueOf(Button)) != null
                    DataPacket data;
                    try {
                        data = new DataPacket(Button, serverManager.getGiveShooter(), serverManager.getCooldown(), serverManager.getItemType(), serverManager.getCheck());
                        objectOutputStream.writeObject(data);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("You: " + data.getCommand());
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

        } catch (IOException | InterruptedException e) {
            System.out.println("Connection lost: " + clientSocket.getInetAddress());
            MultiRoomScreen.getErrorCheck(1);
        }
    }

    public void setIp(){
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        hostIp = inetAddress.getHostAddress();
                        System.out.println("내부 IP 주소: " + inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setButton(String button) {
        Button = button;
    }

    public String getHostIp() {
        return hostIp;
    }

    public int getPort() {
        return MAIN_PORT;
    }

    public static boolean checkConnect() {
        return checkConnect;
    }

    public static int getReturnCode() {
        return returnCode;
    }
}
